var login  = require('./login');
var signup = require('./signup');
var User   = require('../models/userSchema');

module.exports = function(passport){
  passport.serializeUser(function(user, done) {
    done(null, user.id);
  });
  passport.deserializeUser(function(id, done) {
    User.findById(id, function(err, user) { 
      done(err, user);
    });
  });
  login(passport);
  signup(passport);
}
