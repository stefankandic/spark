#include <jni.h>
#include <iostream>
#include <map>
#include <unicode/coll.h>
#include <unicode/ustream.h>
#include "org_apache_spark_sql_catalyst_util_MyClass.h"

#include <unicode/ucnv.h>
#include <unicode/unum.h>

using namespace std;

JNIEXPORT void JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_sayHello
  (JNIEnv *, jclass) {
      std::cout << "Hello from C++ !!" << std::endl;
  }


class SimpleClass {
private:
    string str;

public:
    SimpleClass(string str) {
        this->str = str;
    }

    void printString() {
        cout << this->str << endl;
    }
};

JNIEXPORT jlong JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_createCollator
  (JNIEnv *env, jclass, jstring jlocale) {
    const char* utf8Locale = env->GetStringUTFChars(jlocale, nullptr);
    std::string localeString(utf8Locale);
    env->ReleaseStringUTFChars(jlocale, utf8Locale);

    size_t delimiterPos = localeString.find('-');

    if (delimiterPos == std::string::npos) {
        std::cerr << "Error: Delimiter not found in the locale string." << std::endl;
        return reinterpret_cast<jlong>(nullptr);
    }

    std::string localePart = localeString.substr(0, delimiterPos);
    std::string strengthPart = localeString.substr(delimiterPos + 1);

    icu::Locale icuLocale(localePart.c_str());

    icu::Collator::ECollationStrength collationStrength = icu::Collator::PRIMARY;
    if (strengthPart == "primary") {
        collationStrength = icu::Collator::PRIMARY;
    } else if (strengthPart == "secondary") {
        collationStrength = icu::Collator::SECONDARY;
    } else if (strengthPart == "tertiary") {
        collationStrength = icu::Collator::TERTIARY;
    } else if (strengthPart == "identical") {
        collationStrength = icu::Collator::IDENTICAL;
    } else {
        std::cerr << "Error: Invalid collation strength." << std::endl;
        return reinterpret_cast<jlong>(nullptr);
    }

    UErrorCode status = U_ZERO_ERROR;
    icu::Collator* collator = icu::Collator::createInstance(icuLocale, status);
    collator->setStrength(collationStrength);

    if (U_FAILURE(status)) {
        std::cerr << "Error creating collator: " << u_errorName(status) << std::endl;
        return reinterpret_cast<jlong>(nullptr);
    }

    return reinterpret_cast<jlong>(collator);
  }

JNIEXPORT jint JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_compare
  (JNIEnv *env, jclass, jlong handle, jstring first, jstring second) {
    // return 0;
    icu::Collator* collator = reinterpret_cast<icu::Collator*>(handle);

    jboolean isCopyFirst = JNI_FALSE;
    const char* utf8First = env->GetStringUTFChars(first, &isCopyFirst);
    const char* utf8Second = env->GetStringUTFChars(second, nullptr);
    if (isCopyFirst == JNI_TRUE) {
        std::cout << "The string was copied." << std::endl;
    } else {
        std::cout << "The string is a direct pointer to the characters." << std::endl;
    }
    // const jchar* utf8First = env->GetStringCritical(first, nullptr);
    // const jchar* utf8Second = env->GetStringCritical(second, nullptr);

    UErrorCode status = U_ZERO_ERROR;
    int result = collator->compareUTF8(utf8First, utf8Second, status);

    env->ReleaseStringUTFChars(first, utf8First);
    env->ReleaseStringUTFChars(second, utf8Second);

    if (U_FAILURE(status)) {
        std::cerr << "Error comparing strings: " << u_errorName(status) << std::endl;
        return -1;
    }

    return result;
  }

  JNIEXPORT jint JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_compare2
  (JNIEnv *env, jclass, jlong handle, jbyteArray first, jbyteArray second) {
    icu::Collator* collator = reinterpret_cast<icu::Collator*>(handle);

    jboolean isCopyFirst = JNI_FALSE;
    jboolean isCopySecond = JNI_FALSE;
    char* utf8First = static_cast<char*>(env->GetPrimitiveArrayCritical(first, &isCopyFirst));
    char* utf8Second = static_cast<char*>(env->GetPrimitiveArrayCritical(second, &isCopySecond));
    icu_73::StringPiece sp1(utf8First, env->GetArrayLength(first));
    icu_73::StringPiece sp2(utf8Second, env->GetArrayLength(second));

    if (isCopyFirst == JNI_TRUE || isCopySecond == JNI_TRUE) {
      cout << "COPIED THE ARRAY" << endl;
    }

    UErrorCode status = U_ZERO_ERROR;
    int result = collator->compareUTF8(sp1, sp2, status);

    env->ReleasePrimitiveArrayCritical(second, utf8Second, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(first, utf8First, JNI_ABORT);
    return result;
  }

  //   JNIEXPORT jint JNICALL Java_org_apache_spark_sql_catalyst_util_MyClass_compare2
  // (JNIEnv *env, jclass, jlong handle, jbyteArray first, jbyteArray second) {
  //   icu::Collator* collator = reinterpret_cast<icu::Collator*>(handle);

  //   jboolean isCopyFirst = JNI_FALSE;
  //   jboolean isCopySecond = JNI_FALSE;
  //   jbyte* fbyte = env->GetByteArrayElements(first, &isCopyFirst);
  //   jbyte* sbyte = env->GetByteArrayElements(second, &isCopySecond);

  //   char* utf8First = reinterpret_cast<char*>(fbyte);
  //   char* utf8Second = reinterpret_cast<char*>(sbyte);
  //   icu_73::StringPiece sp1(utf8First, env->GetArrayLength(first));
  //   icu_73::StringPiece sp2(utf8Second, env->GetArrayLength(second));

  //   UErrorCode status = U_ZERO_ERROR;
  //   int result = collator->compareUTF8(sp1, sp2, status);

  //   env->ReleaseByteArrayElements(second, sbyte, JNI_ABORT);
  //   env->ReleaseByteArrayElements(first, fbyte, JNI_ABORT);
  //   return result;
  // }