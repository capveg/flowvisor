# Because I am old and crotchety and my fingers can't stop from running 
#	`make` commands
all:
	ant

docs:
	ant javadoc

doc:
	ant javadoc

run:
	java -jar dist/flowvisor.jar

count: 
	@find src -name \*.java | xargs wc -l | sort -n

clean:
	ant clean
