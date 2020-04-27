from tensorflow import lite
converter = lite.TFLiteConverter.from_keras_model_file( 'trained_model.h5')
tfmodel = converter.convert()
open ("model.tflite" , "wb") .write(tfmodel)