import sys
from PIL import Image

TILESIZE = 128

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print "Usage: [sheet.png] [number] [sub.png]"
    im = Image.open(sys.argv[1])
    at = int(sys.argv[2])
    sub = Image.open(sys.argv[3])
    xs, ys = im.size
    row = at / (xs / TILESIZE)
    col = at % (xs / TILESIZE)
    tobox = (col * TILESIZE, row * TILESIZE, (col+1) * TILESIZE, (row+1) * TILESIZE)
    im.paste(sub, tobox)
    im.save(sys.argv[1])
    

