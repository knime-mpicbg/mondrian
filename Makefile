MONDRIAN_SRC:=$(shell ./parsepb Mondrian.pbproj)

ifeq ($(JDKVER),)
JDKVER:=1.4
endif
JFLAGS=-encoding ISO-8859-1 -target $(JDKVER) -source $(JDKVER)
JAVAC=javac $(JFLAGS)

TARGETS=Mondrian.jar

all: $(TARGETS)

Mondrian.jar: $(MONDRIAN_SRC)
	rm -f *.class
	jar fc MRJ.jar com
	$(JAVAC) -classpath MRJ.jar:. $^
	rm -rf org
	mkdir org
	mkdir org/rosuda/
	mkdir org/rosuda/JRclient
	cp ../../rosuda/JRclient/*.class org/rosuda/JRclient
	jar fcm $@ Mondrian.mft *.class *.gif com org

docs: $(MONDRIAN_SRC)
	mkdir -p JavaDoc
	javadoc -d JavaDoc -author -version -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api $^

clean:
	rm -rf JavaDoc org build
	rm -f $(TARGETS) *.class *~ ../../rosuda/JRclient/*.class ../../rosuda/util/*.class MRJ.jar

.PHONY: clean all
