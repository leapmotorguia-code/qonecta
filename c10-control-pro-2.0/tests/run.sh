set -eu
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
javac -d "$D" app/src/main/java/com/leapmotorguia/c10control/Performance.java tests/PerformanceTest.java
java -cp "$D" PerformanceTest
