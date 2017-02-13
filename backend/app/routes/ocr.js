var tesseract = require('node-tesseract');
var lwip = require('lwip');

/*
 * Open an image from the file system and extract the text in the image
 * If success, call callback with extracted text as parameter
 *
 * Example usage:
 * var ocr = require('./ocr');
 * ocr.extractText('/home/usr/Downloads/testFile.png', function(text) {
 *   console.log(text);
 * });
 */
exports.extractText = function(filePath, callback) {
  tesseract.process(filePath,function(err, text) {
    if(err) console.error('Error while extracting text from image: ' + err.stack);
    if (callback) callback(text);
  });
};
/*
 * Save the 200x200 thumbnail to file system
 *
 * Example usage:
 * ocr.saveThumbnail('/home/usr/Downloads/testFile.png', '/home/usr/Documents/project/testFile.png');
 *
 * or with a callback:
 * ocr.saveThumbnail('/home/usr/Downloads/testFile.png', '/home/usr/Documents/project/testFile.png',
 * function() {
 *  console.log('Thumbnail saved.');
 * });
 */
exports.saveThumbnail = function(filePath, thumbPath, callback) {
  lwip.open(filePath, function(err, image) {
    if (err) console.error('Thumbnail error while opening image: ' + err.stack);

    image.resize(200, 200, function(err, image) {
      if (err)
        console.error('Error while resizing image to thumbnail: ' + err.stack);
      else {
        image.writeFile(thumbPath, function(err) {
          if (err) console.error('Error while saving thumbnail: ' + err.stack);
          if (callback) callback();
        });
      }
    });
  });
};

/*
 * Returns statistics based on an array of ocr calculation times
 */
exports.calculateOcrStatistics = function(timeData) {
  // Cast to array if needed
  if (timeData.length == 1) timeData = [timeData];
  var maxTime = Math.max.apply(Math, timeData);

  var indexOfMax = timeData.indexOf(maxTime);

  var minTime = Math.min.apply(Math, timeData);
  var indexOfMin = timeData.indexOf(minTime);

  var average = calculateAverage(timeData);
  var stdev = calculateStandardDeviation(timeData);

  return {
    maxTime: truncateDecimals(maxTime, 3),
    indexOfMax: indexOfMax,
    minTime: truncateDecimals(minTime, 3),
    indexOfMin: indexOfMin,
    average: truncateDecimals(average, 3),
    stdev: truncateDecimals(stdev, 3)
  };
};

calculateStandardDeviation = function(input) {
  var average = calculateAverage(input);
  var squaredDiffrences = input.map(function(val) { return Math.pow(val-average, 2); });
  var averageOfDiffs = calculateAverage(squaredDiffrences);
  var standardDeviation = Math.sqrt(averageOfDiffs);
  return standardDeviation;
};

calculateAverage = function(input) {
  return input.reduce(function(sum, value) {
    return sum + value;
  }, 0) / input.length;
};

truncateDecimals = function (number, digits) {
  var multiplier = Math.pow(10, digits),
  adjustedNum = number * multiplier,
  truncatedNum = Math[adjustedNum < 0 ? 'ceil' : 'floor'](adjustedNum);

  return truncatedNum / multiplier;
};
