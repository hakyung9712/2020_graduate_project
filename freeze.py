# freeze.py
from tensorflow import keras
model = keras.models.load_model("trained_model.h5", compile=False)

export_path = 'model/'
model.save(export_path, save_format="tf")