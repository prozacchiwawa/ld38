import sys
from PIL import Image

TILESIZE = 128

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: [sheet.png] [desc.txt]")
        sys.exit(1)
    im = Image.open(sys.argv[1])
    desc = open(sys.argv[2]).readlines()
    for line in desc:
        words = line.strip().split()
        if len(words) < 2:
            continue
        at = int(words[0])
        sub = Image.open(words[1])
        xs, ys = im.size
        row = int(at / (xs / TILESIZE))
        col = int(at % (xs / TILESIZE))
        tobox = (col * TILESIZE, row * TILESIZE, (col+1) * TILESIZE, (row+1) * TILESIZE)
        im.paste(sub, tobox)
    im.save(sys.argv[1])
    

