import cv2 
import os 
import time
import sys 


def get_frames(path):
    cap = cv2.VideoCapture(path)
    frame_rate = 170
    prev = time.time()
    i = 0
    if not os.path.exists('./data')  :
        os.mkdir('./data')
    while cap.isOpened():
        time_elapsed = time.time() - prev
        ret, frame = cap.read()
        if not ret:
            break

        if time_elapsed > 1./frame_rate:
            # print(time_elapsed)
            prev = time.time()
            cv2.imwrite('./data/img_'+str(i)+'.jpg', frame)
            i += 1

    cap.release()
    cv2.destroyAllWindows()

get_frames(str(sys.argv[1]))