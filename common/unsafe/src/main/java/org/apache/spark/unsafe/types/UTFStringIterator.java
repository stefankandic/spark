package org.apache.spark.unsafe.types;

import java.util.LinkedList;
import java.util.List;

public class UTFStringIterator implements CharSequence {
  private final byte[] utf8Bytes;
  private final int length;
  private final boolean isLatinOnly;

  // Fields for non-ASCII
  private int decodedUpTo = 0;
  private final char[] characters;
  private final List<Character> ll;

  public UTFStringIterator(byte[] utf8Bytes) {
    this(utf8Bytes, 0, utf8Bytes.length);
  }

  public UTFStringIterator(byte[] utf8Bytes, int offset, int count) {
    this.utf8Bytes = utf8Bytes;

    boolean isLatinOnly = true;
    int numUTF8LeadingBytes = 0;

    for (int i = offset; i < offset + count; i++) {
      if (utf8Bytes[i] < 0) {
        isLatinOnly = false;
      }

      if (isLeadingByte(utf8Bytes[i])) {
        numUTF8LeadingBytes++;
      }
    }

    this.isLatinOnly = isLatinOnly;
    if (this.isLatinOnly) {
      this.length = utf8Bytes.length;
      this.characters = null;
      this.ll = null;
    } else {
      this.length = numUTF8LeadingBytes;
      //TODO: should we just allocate this entire array
      // or try to use something dynamic like ArrayList?
      this.characters = new char[this.length];
      this.ll = new LinkedList<>();
    }
  }

  @Override
  public char charAt(int index) {
    checkIndex(index);

    if (this.isLatinOnly) {
      return (char) (utf8Bytes[index] & 0xff);
    } else if (decodedUpTo > index) {
      var x = ll.get(index);
      return characters[index];
    }

    return getNextCharacter();
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    checkBounds(start, end);

    if (this.isLatinOnly) {
      return new UTFStringIterator(this.utf8Bytes, start, end - start);
    }

    int i = 0, startIdx = 0, endIdx = 0;
    while (i < length) {
      if (i == start) {
        startIdx = i;
      }
      if (i == end) {
        endIdx = i;
        break;
      }

      i += countBytes(utf8Bytes[i]);
    }

    return new UTFStringIterator(this.utf8Bytes, startIdx, endIdx - startIdx);
  }

  @Override
  public int length() {
    return this.length;
  }

  private boolean isLeadingByte(byte b) {
    return (b & 0xC0) != 0x80;
  }

  private char getNextCharacter() {
    int currentPos = decodedUpTo;
    int numBytes = countBytes(utf8Bytes[currentPos]);

    characters[currentPos] = switch (numBytes) {
      case 1 -> (char) (utf8Bytes[decodedUpTo++] & 0xff);
      case 2 -> (char) (((utf8Bytes[decodedUpTo++] & 0x1F) << 6) | (utf8Bytes[decodedUpTo++] & 0x3F));
      case 3 -> (char) (((utf8Bytes[decodedUpTo++] & 0x0F) << 12) | ((utf8Bytes[decodedUpTo++] & 0x3F) << 6)
              | (utf8Bytes[decodedUpTo++] & 0x3F));
      case 4 -> (char) (((utf8Bytes[decodedUpTo++] & 0x07) << 18) | ((utf8Bytes[decodedUpTo++] & 0x3F) << 12)
              | ((utf8Bytes[decodedUpTo++] & 0x3F) << 6) | (utf8Bytes[decodedUpTo++] & 0x3F));
      default -> throw new RuntimeException("Bad leading byte " + utf8Bytes[decodedUpTo]);
    };

    return characters[currentPos];
  }

  private int countBytes(byte leadByte) {
    return UTF8String.bytesOfCodePointInUTF8[leadByte];
//    if (leadByte >= 0) {
//      return 1;
//    } else if (leadByte < (byte) 0xe0) {
//      return leadByte < (byte) 0xc2 ? 0 : 2;
//    } else if (leadByte < (byte) 0xf0) {
//      return 3;
//    } else {
//      return leadByte <= (byte) 0xf4 ? 4 : 0;
//    }
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= length) {
      throw new IndexOutOfBoundsException(
              "index: " + index + ", length: " + length);
    }
  }

  private void checkBounds(int start, int end) {
    if (start < 0 || start > end || end > this.length) {
      throw new IndexOutOfBoundsException(
              "start: " + start + ", end: " + end + ", length: " + this.length);
    }
  }
}
