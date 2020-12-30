all: copy out/production/ld38/index.js

copy: out/production/ld38/index.html \
	out/production/ld38/sprites.png

out/production/ld38/index.js: out/production/ld38/ld38.js
	echo 'var kotlin = require("./lib/kotlin.js")' > $@
	cat $< >> $@

out/production/ld38/index.html: index.html
	cp index.html out/production/ld38

out/production/ld38/sprites.png: assets/sprites.png assets/desc.txt assets/intosheet.py
	python3 assets/intosheet.py assets/sprites.png assets/desc.txt
	cp assets/sprites.png $@
