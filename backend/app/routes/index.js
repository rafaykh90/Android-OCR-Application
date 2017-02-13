var express = require('express');
var router = express.Router();
var passport = require('passport');
var waterfall = require('async-waterfall');
var async = require('async');
var fs = require('fs');
var path = require('path');
var Grid = require('gridfs-stream');
Grid.mongo = require('mongoose').mongo;
var conn = require('mongoose').connection;
var User = require('../models/userSchema');
var Image = require('../models/imageSchema');
var Data = require('../models/imageDataSchema');
var ocr = require('./ocr');

/* Used to protect routes. */
var isAuthenticated = function (req, res, next) {
  if (req.isAuthenticated())
    return next();
  res.redirect('/');
}

var CLIENT_ID = '552032692623-j1355amna6ntrihmc3psl6of826ov2hk.apps.googleusercontent.com'
var CLIENT_SECRET = 'UNUSED'
var REDIRECT_URL = 'UNUSED'

var google = require('googleapis')
var OAuth2 = google.auth.OAuth2
var oauth2Client = new OAuth2(CLIENT_ID, CLIENT_SECRET, REDIRECT_URL)

var verifyGoogleToken = function(req, res, next) {
  var id_token = req.get('id_token')

  if (id_token == null) {
    //res.sendStatus(403)
    req.user_id = 'mockuserid'
    next(null)
  } else {
    oauth2Client.verifyIdToken(id_token, CLIENT_ID, function (err, login) {
      if (err) {
        res.sendStatus(401)
      } else {
        // pass the user ID of the Google Account to the next callback
        req.user_id = login.getPayload().sub
        next(null)
      }
    })
  }
}

/**
  * POST login. Requires in POST body a json object.
  * {username: String, password: String}
  */
router.post('/loginlocal',
  passport.authenticate('login', {
    successRedirect: '/validuser', failureRedirect: '/invaliduser', failureFlash : true
  })
);

/**
  * POST signup. Requires in POST body a json object.
  * {username: String, password: String}
  */
router.post('/signuplocal',
  passport.authenticate('signup', {
    successRedirect: '/validuser', failureRedirect: '/invaliduser', failureFlash : true
  })
);

/* User authenticated. signup/login successful. Returns all user specific records on success */
/*router.get('/validuser', isAuthenticated, function(req, res){
  var query = Data.find({username: req.user.username});
  query.sort({createdAt: 'asc'});
  query.exec(function(error, docs){
    if (error) {
      res.json({messgae: error});
    } else {
      res.json({message: docs});
    }
  });
});*/

/* User authenticated. signup/login successful. Returns all user specific records on success */
router.post('/history', verifyGoogleToken, function(req, res){
  var query = Data.find({username: req.user_id});
  query.sort({createdAt: 'asc'});
  query.exec(function(error, docs){
    if (error) {
      res.json({messgae: error});
    } else {
      res.json({message: docs});
    }
  });
});

/* If user login/signup credentails are not valid. */
router.get('/invaliduser', function(req, res){
  res.json({'success': false});
});

/* POST images. Requires images in POST body as form-data */
router.post('/ocr', verifyGoogleToken, function(req, res){
  var images = req.files;           //All images
  var keys = Object.keys(images);   //Each key corresponds to one image
  if (keys.length === 0) {
    return res.json({message: 'No image to process.'});
  }
  var imageNames = [];
  var thumbnails = [];        //Image thubnails as base64
  var imageText = [];         //Contains Tesseract text output
  var imageSizesInBytes = [];
  var ocrTimesInSeconds = [];
  var imageStatistics = {};
  var uploadLocation = __dirname + '/uploads/';
  var imageLocation = uploadLocation + 'fullsize/';
  var thumbLocation = uploadLocation + 'thumbs/';

  if (!fs.existsSync(uploadLocation)) {
    fs.mkdirSync(uploadLocation)
  }
  if (!fs.existsSync(imageLocation)) {
    fs.mkdirSync(imageLocation)
  }
  if (!fs.existsSync(thumbLocation)) {
    fs.mkdirSync(thumbLocation)
  }

  waterfall([
    /* Create thumbnails from images */
    function(next) {
      async.forEach(keys, function (item, callback){
        imageNames.push(images[item].name);
        imageSizesInBytes.push(images[item].size);
        fs.writeFile(imageLocation+''+images[item].name, images[item].data, function (err) {
          if (err) {
            next({message: 'Error writing file: ' + err});
          } else {
            ocr.saveThumbnail(imageLocation+''+images[item].name, thumbLocation+''+images[item].name, callback);
          }
        });
      }, function() {
        next(null);
      });
    },
    /* Read thumbnails and store as base64 in an array */
    function(next) {
      async.forEach(keys, function (item, callback){
        fs.readFile(thumbLocation+''+images[item].name, function (err,data) {
          if (err) {
            //console.log(err);
            callback({message: 'Error reading thumbnail: '+err});
          } else {
            thumbnails.push(new Buffer(data).toString('base64'));
            callback();
          }
        });
      }, function() {
        next(null);
      });
    },
    /* Extract text from images and store the text in array. Calculate bench-mark values */
    function(next) {
      async.forEach(keys, function (item, callback){
        var startTime = Date.now();
        ocr.extractText(imageLocation+''+images[item].name, function(text) {
          var endTime = Date.now();
          var time = (endTime - startTime) / 1000;
          ocrTimesInSeconds.push(time);
          imageText.push(text);
          //console.log(text);
          callback();
        });
      }, function() {
        imageStatistics = ocr.calculateOcrStatistics(ocrTimesInSeconds);
        imageStatistics.ocrTimesInSeconds = ocrTimesInSeconds;
        imageStatistics.imageSizesInBytes = imageSizesInBytes;
        console.log('imageStatistics: ' + JSON.stringify(imageStatistics));
        next(null);
      });
    },
    /* Save each image to MongoDb in base64 */
    function(next) {
      async.forEach(keys, function (item, callback){ 
        var imageInDB = {
          //username: req.user.username,
          username: req.user_id,
          imageName: images[item].name,
          image: new Buffer(images[item].data).toString('base64') 
        };
        Image.create(imageInDB, function(error, docs){
          if (error){
            next({message: error});
          } else{
            callback();
          }
        });
      }, next(null));
    },
    /* Save image to GridFS */
    /*function(next) {
      async.forEach(keys, function (item, next){
        writeStream = gfs.createWriteStream({
          filename: images[item].name,
          //metadata: req.user.username
          metadata: req.user_id
        });
        writeStream.write(images[item].data);
        writeStream.end();
      }, next(null));
    },*/
    /* Save image related data to MongoDB */
    function(next) {
      var imageInfo = {
        //username: req.user.username,
        username: req.user_id,
        imgText: imageText, /*Scanned text array comes here*/
        imgNames: imageNames, /*Image names array comes here*/
        imgThumbs: thumbnails /*thumbnails array comes here*/
      };
      Data.create(imageInfo, function(error, docs){
        if (error){
          next({message: err});
        } else{
          next(null, docs);
        }
      });
    }
  ],
  function(err, result) {
    if (err) {
      res.json(err);
    } else {
      //console.log(result);
      res.json({message: result, imageStatistics: imageStatistics});
    }
  });
});


/* GET an image by name. Returns image in base64 */
router.get('/image/:imgname', verifyGoogleToken, function(req, res) {
  var query = Image.find({username: req.user_id, imageName: req.params.imgname});
  query.exec(function(error, docs){
    if (error) {
      res.json({messgae: error});
    } else {
      console.log(docs);
      if (docs.length > 0) {
        res.json({message: docs[0].image});
        //res.write(docs[0].image);
        //res.end();
      } else {
        res.json({message: 'No image found'});
      }
    }
  });
});

/* GET image. Requires image name as route parameter */
/*conn.once("open", function() {
  var gfs = Grid(conn.db);
  //router.get('/image/:imgname', isAuthenticated, function(req, res) {
  router.get('/image/:imgname', verifyGoogleToken, function(req, res) {
    gfs.files.find({
      //metadata: req.user.username,
      metadata: req.user_id,
      filename: req.params.imgname
    }).toArray(function(err, files) {
      if (files.length === 0) {
        return res.json({message: 'File not found'});
      }
      console.log(files);
      var data = [];
      var readstream = gfs.createReadStream({
        filename: files[0].filename
      });
      readstream.on('data', function(data) {
        res.write(data);
      });
      readstream.on('end', function() {
        res.end();
      });
      readstream.on('error', function(err) {
        console.log('An error occurred! ', err);
        res.json({message: 'File not found'})
      });
    });
  });
});
*/

/* GET logout page. */
router.get('/logout', function(req, res) {
  req.logout();
  res.json({success: true});  // After logout success, show start page.
});

module.exports = router;
