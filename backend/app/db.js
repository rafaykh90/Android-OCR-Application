var mongoose = require('mongoose');
module.exports = function(){
  mongoose.Promise = global.Promise;

  var mongodb_options = {
    db: {
      readPreference: 'nearest',
    },
    replSet: {
      replicaSet: 'rs0'
    },
    server: {
      autoReconnect: true
    }
  }
  mongoose.connect('mongodb://mongo-1,mongo-2,mongo-3:27017/mccp2', mongodb_options);
  //mongoose.connect('mongodb://localhost:27017/mccp2');
}
