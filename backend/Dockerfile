FROM node:latest

ENV SOURCE_IMAGE_LIFETIME=7

RUN apt-get update
RUN apt-get -y install tesseract-ocr
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY ./app/ ./
RUN npm install

CMD ["npm", "start"]

