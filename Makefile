# Because I am old and crotchety and my fingers can't stop from running 
#	`make` commands
all:
	ant

run:
	java -jar dist/flowvisor.jar

count: 
	@find . -name \*.java | xargs wc -l | sort -n

clean:
	ant clean
