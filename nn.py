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

#각 행의 동일 인덱스의 요소를 그룹으로 하여 그 중 max값
num_classes = np.max(y, axis=0)

#과적합(학습 데이터 안에서는 일정수준 이상의 예측을 하지만 새로운 데이터에선 잘 안맞는거) 방지하기 위해 학습셋/테스트셋 분리
#학습용/ 테스트용 데이터 나누기
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.4, random_state=0)

# Build the Neural Network
model = Sequential()
#dense는 2D 레이어. 다른 노드들과 연결됨.
#첫번째 계층. 입력층(은닉충) . 이 층에 512개의 노드를 만들것이라는 거. 
#input_dim은 몇 개의 값이 들어올지를 결정하는 것. 
#데이터에서 193개의 값을 받아(속성의 갯수) 은닉층의 512 노드로 보낸다는 뜻.
model.add(Dense(512, activation='relu', input_dim=193))
model.add(Dropout(0.5))
model.add(Dense(512, activation='relu'))
model.add(Dropout(0.5))
#출력층. 노드 개수는 클래스의 갯수만큼
#활성화 함수는 softmax.이거는 총합이 1인 형태로 바꿔서 계산해주는 함수
model.add(Dense(num_classes, activation='softmax'))

#모델 학습 전 컴파일 필요. 학습과정을 정의하는 것
#최적화기/손실함수/평가척도.분류인경우 'accuracy'많이 사용
model.compile(optimizer='rmsprop',
              loss='categorical_crossentropy',
              metrics=['accuracy'])

#문자열을 숫자로 바꿔주는 것
#여러개의 문자를 0과 1로만 이루어진 형태로 바꿔준다. 원핫인코딩
# Convert label to onehot
#함수의 인자(클래스,클래스갯수)
y_train = keras.utils.to_categorical(y_train-1, num_classes=num_classes)
y_test = keras.utils.to_categorical(y_test-1, num_classes=num_classes)

#모델 학습
# 각 샘플이 처음부터 끝까지 1000번 재사용될때까지 실행을 반복하라는 뜻.epochs
#샘플을 한번에 몇개씩 처리할지 정하는 부분이 batch_size
model.fit(X_train, y_train, epochs=1000, batch_size=64)
score, acc = model.evaluate(X_test, y_test, batch_size=32)
print('Test score:', score)
print('Test accuracy:', acc)
