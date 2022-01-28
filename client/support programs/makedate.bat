rem make date file
rem this should be scheduled to run daily using taskschd.msc
echo package lib; public interface Timestamp { String build =  "%date%" ;} > g:\share\projects\boardspace-java\boardspace-core\lib\Timestamp.java
copy "g:\share\projects\boardspace-java\boardspace-core\lib\Timestamp.java" "g:\share\projects\boardspace-codename1\boardspace core\lib\Timestamp.java"
