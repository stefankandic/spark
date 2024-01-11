#!/bin/bash

# Compile the source file to object file
g++ -c -O3 -DNDEBUG -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -I/opt/homebrew/opt/icu4c/include -std=c++11 org_apache_spark_sql_catalyst_util_MyClass.cpp -o org_apache_spark_sql_catalyst_util_MyClass.o

# Create the dynamic library
g++ -O3 -DNDEBUG -dynamiclib -o libnative.dylib org_apache_spark_sql_catalyst_util_MyClass.o -L/opt/homebrew/opt/icu4c/lib -licuuc -licui18n

echo "Compilation completed."
