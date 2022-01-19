@echo off
set JUNIT_HOME=P:\test-libs
%JAVA_HOME%\bin\java -classpath "pf-joi-full.jar;pf-joi-test.jar;%JUNIT_HOME%\junit.jar" org.pf.joi.AllTests
