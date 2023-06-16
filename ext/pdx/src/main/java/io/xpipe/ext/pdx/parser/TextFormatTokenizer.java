package io.xpipe.ext.pdx.parser;

import java.util.Arrays;
import java.util.Stack;

public class TextFormatTokenizer {

    public static final byte STRING_UNQUOTED = 1;
    public static final byte STRING_QUOTED = 2;
    public static final byte OPEN_GROUP = 3;
    public static final byte CLOSE_GROUP = 4;
    public static final byte EQUALS = 5;

    private static final byte DOUBLE_QUOTE_CHAR = 34;
    private static final byte SPACE_CHAR = 32;
    private static final byte EQUALS_CHAR = 61;

    private static final byte[] UTF_8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final String name;
    private final boolean strict;
    private final byte[] bytes;
    private final Stack<Integer> arraySizeStack;
    private byte[] tokenTypes;
    private int[] scalarsStart;
    private short[] scalarsLength;
    private int[] arraySizes;
    private boolean isInQuotes;
    private boolean isInComment;
    private int nextScalarStart;
    private int i;
    private int tokenCounter;
    private int scalarCounter;
    private int arraySizesCounter;
    private boolean escapeChar;

    public TextFormatTokenizer(String name, byte[] bytes, int start, boolean strict) {
        this.name = name;
        this.bytes = bytes;
        this.strict = strict;
        this.tokenCounter = 0;

        int maxTokenCount;
        int maxNodeCount;
        if (bytes.length < 300) {
            // Special case for small files

            // Add 2 to include open and close group tokens that are always added
            maxTokenCount = bytes.length + 2;

            // Add 1 in case bytes.length is 0. We then still have one empty array node
            maxNodeCount = bytes.length + 1;
        } else {
            // Pessimistic assumptions, should always hold!

            maxTokenCount = bytes.length / 2;
            maxNodeCount = bytes.length / 5;
        }

        this.tokenTypes = new byte[maxTokenCount];
        this.scalarsStart = new int[maxNodeCount];
        this.scalarsLength = new short[maxNodeCount];
        this.arraySizes = new int[maxNodeCount];

        this.arraySizeStack = new Stack<>();
        this.arraySizesCounter = 0;

        this.i = start;
        this.nextScalarStart = start;
    }

    private void checkResize() {
        var maxTokenCount = tokenTypes.length;
        var maxNodeCount = scalarsStart.length;
        if (this.tokenCounter >= maxTokenCount || this.scalarCounter >= maxNodeCount) {
            resize();
        }
    }

    private void resize() {
        var maxTokenCount = tokenTypes.length;
        var maxNodeCount = scalarsStart.length;

        var tokenTypes = new byte[maxTokenCount * 2];
        System.arraycopy(this.tokenTypes, 0, tokenTypes, 0, maxTokenCount);
        this.tokenTypes = tokenTypes;

        var scalarsStart = new int[maxNodeCount * 2];
        System.arraycopy(this.scalarsStart, 0, scalarsStart, 0, maxNodeCount);
        this.scalarsStart = scalarsStart;

        var scalarsLength = new short[maxNodeCount * 2];
        System.arraycopy(this.scalarsLength, 0, scalarsLength, 0, maxNodeCount);
        this.scalarsLength = scalarsLength;

        var arraySizes = new int[maxNodeCount * 2];
        System.arraycopy(this.arraySizes, 0, arraySizes, 0, maxNodeCount);
        this.arraySizes = arraySizes;
    }

    private void checkBom() {
        if (bytes.length >= 3 && Arrays.equals(bytes, 0, 3, UTF_8_BOM, 0, 3)) {
            this.nextScalarStart += 3;
            this.i += 3;
        }
    }

    private void checkUnclosedArrays() throws ParseException {
        if (strict && arraySizeStack.size() > 1) {
            throw ParseException.createFromOffset(name, "Missing closing } at the end of the file", i - 1, bytes);
        }

        for (int i = 1; i < arraySizeStack.size(); i++) {
            tokenTypes[tokenCounter] = CLOSE_GROUP;
            tokenCounter++;
        }
        arraySizeStack.clear();
    }

    public void tokenize() throws ParseException {
        tokenTypes[0] = OPEN_GROUP;
        arraySizes[0] = 0;
        arraySizeStack.add(0);
        arraySizesCounter++;
        tokenCounter = 1;
        checkBom();
        for (; i <= bytes.length; i++) {
            tokenizeIteration();
        }
        checkUnclosedArrays();
        tokenTypes[tokenCounter] = CLOSE_GROUP;
    }

    private void moveScalarStartToNext() {
        nextScalarStart = i + 1;
    }

    private boolean checkCommentCase(char c) throws ParseException {
        if (isInQuotes) {
            return false;
        }

        if (!isInComment) {
            if (c == '#') {
                isInComment = true;
                finishCurrentToken();
                return true;
            } else {
                return false;
            }
        }

        if (c == '\n') {
            isInComment = false;
            moveScalarStartToNext();
        }
        return true;
    }

    private boolean checkQuoteCase(char c) throws ParseException {
        if (!isInQuotes) {
            if (c == '"') {
                isInQuotes = true;
                finishCurrentToken();
                return true;
            }

            return false;
        }

        boolean hasSeenEscapeChar = escapeChar;
        if (hasSeenEscapeChar) {
            escapeChar = false;
            if (c == '"' || c == '\\') {
                return true;
            }
        } else {
            escapeChar = c == '\\';
            if (c == '"') {
                isInQuotes = false;
                finishCurrentToken(i + 1);
            }
        }

        return true;
    }

    private void finishCurrentToken() throws ParseException {
        finishCurrentToken(i);
    }

    private void finishCurrentToken(int endExclusive) throws ParseException {
        boolean isCurrentToken = nextScalarStart < endExclusive;
        if (!isCurrentToken) {
            return;
        }

        short length = (short) ((endExclusive - 1) - nextScalarStart + 1);

        // Check for length overflow
        if (length < 0) {
            throw ParseException.createFromOffset(
                    name,
                    "Encountered scalar with length " + ((endExclusive - 1) - nextScalarStart + 1)
                            + ", which is too big",
                    nextScalarStart,
                    bytes);
        }

        assert length > 0 : "Scalar must be of length at least 1";

        if (bytes[nextScalarStart] == DOUBLE_QUOTE_CHAR && bytes[endExclusive - 1] == DOUBLE_QUOTE_CHAR) {
            tokenTypes[tokenCounter++] = STRING_QUOTED;
        } else {
            tokenTypes[tokenCounter++] = STRING_UNQUOTED;
        }
        scalarsStart[scalarCounter] = nextScalarStart;
        scalarsLength[scalarCounter] = length;
        scalarCounter++;

        assert arraySizeStack.size() > 0 : "Encountered unexpectedly large array at index " + endExclusive;
        arraySizes[arraySizeStack.peek()]++;

        nextScalarStart = endExclusive;
    }

    private byte getSuccessorByte() {
        return i == bytes.length - 1 ? 0 : bytes[i + 1];
    }

    private void setSuccessorByte(byte b) {
        if (i < bytes.length - 1) {
            bytes[i + 1] = b;
        }
    }

    private boolean checkForControlTokenKey(byte controlToken) {
        if (controlToken == CLOSE_GROUP) {
            if (getSuccessorByte() == EQUALS_CHAR) {
                if (nextScalarStart == i) {
                    return true;
                }
            }
        }

        if (controlToken == OPEN_GROUP) {
            if (getSuccessorByte() == EQUALS_CHAR) {
                if (nextScalarStart == i) {
                    return true;
                }
            }
        }

        if (controlToken == EQUALS) {
            if (getSuccessorByte() == EQUALS_CHAR) {
                if (nextScalarStart == i) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkForNewControlToken(byte controlToken) throws ParseException {
        if (controlToken == 0) {
            return;
        }

        if (checkForControlTokenKey(controlToken)) {
            return;
        }

        finishCurrentToken();
        moveScalarStartToNext();

        if (controlToken == CLOSE_GROUP) {
            // Special case for additional close group token on top level
            // Happens in CK2 and VIC2
            if (arraySizeStack.size() == 1) {
                if (strict) {
                    throw ParseException.createFromOffset(name, "Additional closing } at the of the file", i, bytes);
                }

                return;
            }

            if (getSuccessorByte() == EQUALS) {
                if (strict) {
                    throw ParseException.createFromOffset(name, "Invalid key name }", i, bytes);
                }

                setSuccessorByte(SPACE_CHAR);
                return;
            }

            arraySizeStack.pop();
        } else if (controlToken == EQUALS) {
            if (strict && arraySizes[arraySizeStack.peek()] == 0) {
                throw ParseException.createFromOffset(name, "Encountered invalid =", i, bytes);
            }

            if (arraySizes[arraySizeStack.peek()] > 0) {
                arraySizes[arraySizeStack.peek()]--;
            }
        } else if (controlToken == OPEN_GROUP) {
            arraySizes[arraySizeStack.peek()]++;
            arraySizeStack.add(arraySizesCounter++);
        }

        tokenTypes[tokenCounter++] = controlToken;
    }

    private void checkWhitespace(char c) throws ParseException {
        boolean isWhitespace = (c == '\n' || c == '\r' || c == ' ' || c == '\t');
        if (isWhitespace) {
            finishCurrentToken();
            moveScalarStartToNext();
        }
    }

    private void tokenizeIteration() throws ParseException {
        // Add extra new line at the end to simulate end of token
        char c = i == bytes.length ? '\n' : (char) bytes[i];

        if (checkCommentCase(c)) return;
        if (checkQuoteCase(c)) return;

        checkResize();

        byte controlToken = 0;
        if (c == '{') {
            controlToken = OPEN_GROUP;
        } else if (c == '}') {
            controlToken = CLOSE_GROUP;
        } else if (c == '=') {
            controlToken = EQUALS;
        }

        checkForNewControlToken(controlToken);
        checkWhitespace(c);
    }

    public byte[] getTokenTypes() {
        return tokenTypes;
    }

    public int[] getArraySizes() {
        return arraySizes;
    }

    public int[] getScalarsStart() {
        return scalarsStart;
    }

    public short[] getScalarsLength() {
        return scalarsLength;
    }

    public int getScalarCount() {
        return scalarCounter;
    }
}
