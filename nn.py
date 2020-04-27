# coding= UTF-8
#
# Author: Fing
# Date  : 2017-12-03
#

import numpy as np
import keras
from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation
from keras.optimizers import SGD
from sklearn.model_selection import train_test_split

# Prepare the data
X =  np.load('feat.npy')
y =  np.load('label.npy').ravel()

#�� ���� ���� �ε����� ��Ҹ� �׷����� �Ͽ� �� �� max��
num_classes = np.max(y, axis=0)

#������(�н� ������ �ȿ����� �������� �̻��� ������ ������ ���ο� �����Ϳ��� �� �ȸ´°�) �����ϱ� ���� �н���/�׽�Ʈ�� �и�
#�н���/ �׽�Ʈ�� ������ ������
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.4, random_state=0)

# Build the Neural Network
model = Sequential()
#dense�� 2D ���̾�. �ٸ� ����� �����.
#ù��° ����. �Է���(������) . �� ���� 512���� ��带 ������̶�� ��. 
#input_dim�� �� ���� ���� �������� �����ϴ� ��. 
#�����Ϳ��� 193���� ���� �޾�(�Ӽ��� ����) �������� 512 ���� �����ٴ� ��.
model.add(Dense(512, activation='relu', input_dim=193))
model.add(Dropout(0.5))
model.add(Dense(512, activation='relu'))
model.add(Dropout(0.5))
#�����. ��� ������ Ŭ������ ������ŭ
#Ȱ��ȭ �Լ��� softmax.�̰Ŵ� ������ 1�� ���·� �ٲ㼭 ������ִ� �Լ�
model.add(Dense(num_classes, activation='softmax'))

#�� �н� �� ������ �ʿ�. �н������� �����ϴ� ��
#����ȭ��/�ս��Լ�/��ô��.�з��ΰ�� 'accuracy'���� ���
model.compile(optimizer='rmsprop',
              loss='categorical_crossentropy',
              metrics=['accuracy'])

#���ڿ��� ���ڷ� �ٲ��ִ� ��
#�������� ���ڸ� 0�� 1�θ� �̷���� ���·� �ٲ��ش�. �������ڵ�
# Convert label to onehot
#�Լ��� ����(Ŭ����,Ŭ��������)
y_train = keras.utils.to_categorical(y_train-1, num_classes=num_classes)
y_test = keras.utils.to_categorical(y_test-1, num_classes=num_classes)

#�� �н�
# �� ������ ó������ ������ 1000�� ����ɶ����� ������ �ݺ��϶�� ��.epochs
#������ �ѹ��� ��� ó������ ���ϴ� �κ��� batch_size
model.fit(X_train, y_train, epochs=1000, batch_size=64)
score, acc = model.evaluate(X_test, y_test, batch_size=32)
print('Test score:', score)
print('Test accuracy:', acc)
