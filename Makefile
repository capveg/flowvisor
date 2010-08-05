# Because I am old and crotchety and my fingers can't stop from running 
#	`make` commands

.PHONY: docs doc all test tests count install clean

all:
	ant

docs:
	ant javadoc

doc:
	ant javadoc

test: all
	make -C regress tests
tests: all
	make -C regress tests

count: 
	@find src -name \*.java | xargs wc -l | sort -n

install: all
	./scripts/install-script.sh

clean:
	ant clean
