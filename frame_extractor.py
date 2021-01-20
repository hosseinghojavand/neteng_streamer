import cv2 
import os 
import time
import sys 


def get_frames(input_path , output_path):
    cap = cv2.VideoCapture(input_path)
    frame_rate = 170
    prev = time.time()
    i = 0
    if not os.path.exists(output_path)  :
        os.mkdir(output_path)
    while cap.isOpened():
        time_elapsed = time.time() - prev
        ret, frame = cap.read()
        if not ret:
            break

        if time_elapsed > 1./frame_rate:
            # print(time_elapsed)
            prev = time.time()
            cv2.imwrite(output_path + '/img_'+str(i)+'.jpg', frame)
            i += 1
    print(i)

    cap.release()
    cv2.destroyAllWindows()

get_frames(str(sys.argv[1]) , str(sys.argv[2]))
