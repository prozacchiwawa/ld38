all: copy

copy: out/production/ld38/index.html \
	out/production/ld38/sprites.png

out/production/ld38/index.html: index.html
	cp index.html out/production/ld38

out/production/ld38/sprites.png: assets/sprites.png assets/desc.txt assets/intosheet.py
	python assets/intosheet.py assets/sprites.png assets/desc.txt
	cp assets/sprites.png $@
