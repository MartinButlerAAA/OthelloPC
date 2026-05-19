del *.bak
del *.class
javac *.java
"C:\Program Files\Java\jdk-23\bin\jar.exe" cfm Othello.jar mf.txt *.class *.jpg
del *.class
pause
cd .
java -jar Othello.jar
pause