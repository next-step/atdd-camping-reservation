$env:JAVA_HOME = 'C:\Users\USER\.jdks\corretto-17.0.15'
$env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
.\gradlew.bat test jacocoTestReport