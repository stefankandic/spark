/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_apache_spark_sql_catalyst_util_MyClass */

#ifndef _Included_org_apache_spark_sql_catalyst_util_MyClass
#define _Included_org_apache_spark_sql_catalyst_util_MyClass
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_apache_spark_sql_catalyst_util_MyClass
 * Method:    createCollator
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jlong JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_createCollator
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_apache_spark_sql_catalyst_util_MyClass
 * Method:    compare
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_compare
  (JNIEnv *, jclass, jlong, jstring, jstring);

  /*
 * Class:     org_apache_spark_sql_catalyst_util_MyClass
 * Method:    compare
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_compare2
  (JNIEnv *, jclass, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_apache_spark_sql_catalyst_util_MyClass
 * Method:    sayHello
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_sayHello
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
