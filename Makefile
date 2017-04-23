all: copy

copy: out/production/ld38/index.html \
	out/production/ld38/sprites.png

out/production/ld38/index.html: index.html
	cp index.html out/production/ld38

out/production/ld38/sprites.png: assets/sprites.png
	python assets/intosheet.py assets/sprites.png 20 assets/Door0001.png
	python assets/intosheet.py assets/sprites.png 21 assets/Door0002.png
	python assets/intosheet.py assets/sprites.png 22 assets/Door0003.png
	python assets/intosheet.py assets/sprites.png 23 assets/Door0004.png
	python assets/intosheet.py assets/sprites.png 24 assets/WallCorner.png
	python assets/intosheet.py assets/sprites.png 25 assets/Wall.png
	python assets/intosheet.py assets/sprites.png 26 assets/CornerFloor.png
	python assets/intosheet.py assets/sprites.png 27 assets/EdgeFloor.png
	python assets/intosheet.py assets/sprites.png 28 assets/Floor.png
	cp $< $@
