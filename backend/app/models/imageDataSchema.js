var mongoose = require('mongoose');

var imageDataSchema = new mongoose.Schema({
  username: String,
  createdAt: {type: Date, default: Date.now},
  imgText: Array,
  imgNames: Array,
  imgThumbs: Array
});

module.exports = mongoose.model('Data', imageDataSchema);