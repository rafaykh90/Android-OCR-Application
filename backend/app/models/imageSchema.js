var mongoose = require('mongoose');

/* Read document expiry value */
var lifeTime = process.env.SOURCE_IMAGE_LIFETIME;
var imageSchema = new mongoose.Schema({
  username: String,
  imageName: String,
  createdAt: {type: Date, default: new Date(), expires: lifeTime+'d'},
  image: String
});

module.exports = mongoose.model('Image', imageSchema);