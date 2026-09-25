package com.anlyflad.core.svg;
import com.anlyflad.core.model.Point;
public final class SvgTransform {
    private final double a;
    private final double b;
    private final double c;
    private final double d;
    private final double e;
    private final double f;
    public SvgTransform(double a, double b, double c, double d, double e, double f) {
        validateFinite(a, "a");
        validateFinite(b, "b");
        validateFinite(c, "c");
        validateFinite(d, "d");
        validateFinite(e, "e");
        validateFinite(f, "f");
        this.a=a;
        this.b=b;
        this.c=c;
        this.d=d;
        this.e=e;
        this.f=f;
    }
    public static SvgTransform identity() {
        return new SvgTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
    }
    public static SvgTransform translation(double tx, double ty) {
        return new SvgTransform(1.0, 0.0, 0.0, 1.0, tx, ty);
    }
    public static SvgTransform translate(double tx, double ty) {
        return translation(tx, ty);
    }
    public static SvgTransform translation(double tx) {
        return translation(tx, 0.0);
    }
    public static SvgTransform translate(double tx) {
        return translation(tx);
    }
    public static SvgTransform scale(double sx, double sy) {
        return new SvgTransform(sx, 0.0, 0.0, sy, 0.0, 0.0);
    }
    public static SvgTransform scale(double value) {
        return scale(value, value);
    }
    public static SvgTransform rotation(double degrees) {
        return rotationRadians(Math.toRadians(degrees));
    }
    public static SvgTransform rotate(double degrees) {
        return rotation(degrees);
    }
    public static SvgTransform rotationRadians(double radians) {
        double cosine=Math.cos(radians);
        double sine=Math.sin(radians);
        return new SvgTransform(cosine, sine, -sine, cosine, 0.0, 0.0);
    }
    public static SvgTransform skewX(double degrees) {
        return new SvgTransform(1.0, 0.0, Math.tan(Math.toRadians(degrees)), 1.0, 0.0, 0.0);
    }
    public static SvgTransform skewY(double degrees) {
        return new SvgTransform(1.0, Math.tan(Math.toRadians(degrees)), 0.0, 1.0, 0.0, 0.0);
    }
    public static SvgTransform matrix(double a, double b, double c, double d, double e, double f) {
        return new SvgTransform(a, b, c, d, e, f);
    }
    public static SvgTransform of(double a, double b, double c, double d, double e, double f) {
        return matrix(a, b, c, d, e, f);
    }
    public static SvgTransform concatenate(SvgTransform first, SvgTransform second) {
        if (first==null||second==null) {
            throw new IllegalArgumentException("transforms must not be null");
        }
        return first.concatenate(second);
    }
    public static SvgTransform concat(SvgTransform first, SvgTransform second) {
        return concatenate(first, second);
    }
    public SvgTransform concatenate(SvgTransform next) {
        if (next==null) {
            throw new IllegalArgumentException("next transform must not be null");
        }
        return new SvgTransform(a*next.a+c*next.b, b*next.a+d*next.b, a*next.c+c*next.d, b*next.c+d*next.d, a*next.e+c*next.f+e, b*next.e+d*next.f+f);
    }
    public SvgTransform concat(SvgTransform next) {
        return concatenate(next);
    }
    public SvgTransform then(SvgTransform next) {
        return concatenate(next);
    }
    public SvgTransform multiply(SvgTransform other) {
        return concatenate(other);
    }
    public SvgTransform prepend(SvgTransform previous) {
        if (previous==null) {
            throw new IllegalArgumentException("previous transform must not be null");
        }
        return previous.concatenate(this);
    }
    public Point transform(Point point) {
        if (point==null) {
            throw new IllegalArgumentException("point must not be null");
        }
        return new Point(transformX(point.getX(), point.getY()), transformY(point.getX(), point.getY()));
    }
    public Point transform(double x, double y) {
        validateFinite(x, "x");
        validateFinite(y, "y");
        return new Point(transformX(x, y), transformY(x, y));
    }
    public Point apply(double x, double y) {
        return transform(x, y);
    }
    public double transformX(double x, double y) {
        validateFinite(x, "x");
        validateFinite(y, "y");
        double transformed=a*x+c*y+e;
        validateFinite(transformed, "transformed x");
        return transformed;
    }
    public double transformY(double x, double y) {
        validateFinite(x, "x");
        validateFinite(y, "y");
        double transformed=b*x+d*y+f;
        validateFinite(transformed, "transformed y");
        return transformed;
    }
    public double[] toArray() {
        return new double[]{a, b, c, d, e, f};
    }
    public double getA() {
        return a;
    }
    public double getB() {
        return b;
    }
    public double getC() {
        return c;
    }
    public double getD() {
        return d;
    }
    public double getE() {
        return e;
    }
    public double getF() {
        return f;
    }
    public static SvgTransform parse(String value) throws SvgParseException {
        if (value==null) {
            throw new SvgParseException("transform must not be null");
        }
        int index=skipWhitespace(value, 0);
        if (index==value.length()) {
            return identity();
        }
        SvgTransform result=identity();
        while (index<value.length()) {
            index=skipSeparators(value, index);
            if (index==value.length()) {
                break;
            }
            int nameStart=index;
            while (index<value.length()&&isTransformNameCharacter(value.charAt(index))) {
                index++;
            }
            if (nameStart==index) {
                throw error("expected transform name");
            }
            String name=value.substring(nameStart, index);
            index=skipWhitespace(value, index);
            if (index==value.length()||value.charAt(index)!='(') {
                throw error("expected ( after transform name "+name);
            }
            index++;
            double[] arguments=new double[6];
            int[] cursor=new int[]{index};
            int count=parseArguments(value, cursor, arguments);
            index=cursor[0];
            if (index==value.length()||value.charAt(index)!=')') {
                throw error("expected ) after transform "+name);
            }
            index++;
            try {
                SvgTransform next=createTransform(name, arguments, count);
                result=result.concatenate(next);
            } catch (IllegalArgumentException exception) {
                throw new SvgParseException("invalid transform "+name, exception);
            }
            index=skipWhitespace(value, index);
        }
        return result;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        SvgTransform transform=(SvgTransform)other;
        return bits(a)==bits(transform.a)&&bits(b)==bits(transform.b)&&bits(c)==bits(transform.c)&&bits(d)==bits(transform.d)&&bits(e)==bits(transform.e)&&bits(f)==bits(transform.f);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+longBits(a);
        result=31*result+longBits(b);
        result=31*result+longBits(c);
        result=31*result+longBits(d);
        result=31*result+longBits(e);
        result=31*result+longBits(f);
        return result;
    }
    @Override
    public String toString() {
        return "SvgTransform{a="+a+", b="+b+", c="+c+", d="+d+", e="+e+", f="+f+"}";
    }
    private static SvgTransform createTransform(String name, double[] arguments, int count) throws SvgParseException {
        if (name.equals("matrix")) {
            requireCount(name, count, 6);
            return matrix(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]);
        }
        if (name.equals("translate")) {
            if (count!=1&&count!=2) {
                throw error("translate expects one or two arguments");
            }
            double ty=count==2?arguments[1]:0.0;
            return translation(arguments[0], ty);
        }
        if (name.equals("scale")) {
            if (count!=1&&count!=2) {
                throw error("scale expects one or two arguments");
            }
            double sy=count==2?arguments[1]:arguments[0];
            return scale(arguments[0], sy);
        }
        if (name.equals("rotate")) {
            if (count!=1&&count!=3) {
                throw error("rotate expects one or three arguments");
            }
            SvgTransform rotation=rotation(arguments[0]);
            if (count==3) {
                return translation(arguments[1], arguments[2]).concatenate(rotation).concatenate(translation(-arguments[1], -arguments[2]));
            }
            return rotation;
        }
        if (name.equals("skewX")) {
            requireCount(name, count, 1);
            return skewX(arguments[0]);
        }
        if (name.equals("skewY")) {
            requireCount(name, count, 1);
            return skewY(arguments[0]);
        }
        throw error("unsupported transform "+name);
    }
    private static int parseArguments(String value, int[] cursor, double[] arguments) throws SvgParseException {
        int index=cursor[0];
        int count=0;
        boolean comma=false;
        while (index<value.length()) {
            index=skipWhitespace(value, index);
            if (index<value.length()&&value.charAt(index)==')') {
                if (comma) {
                    throw error("trailing comma in transform arguments");
                }
                cursor[0]=index;
                return count;
            }
            if (index<value.length()&&value.charAt(index)==',') {
                if (comma||count==0) {
                    throw error("invalid comma in transform arguments");
                }
                comma=true;
                index++;
                continue;
            }
            if (count==arguments.length) {
                throw error("too many transform arguments");
            }
            int numberStart=index;
            index=readNumberEnd(value, index);
            if (index==numberStart) {
                throw error("expected transform number");
            }
            arguments[count]=parseNumber(value, numberStart, index);
            count++;
            comma=false;
        }
        throw error("unterminated transform arguments");
    }
    private static int readNumberEnd(String value, int start) {
        int index=start;
        if (index<value.length()&&(value.charAt(index)=='+'||value.charAt(index)=='-')) {
            index++;
        }
        int digits=0;
        while (index<value.length()&&isDigit(value.charAt(index))) {
            index++;
            digits++;
        }
        if (index<value.length()&&value.charAt(index)=='.') {
            index++;
            while (index<value.length()&&isDigit(value.charAt(index))) {
                index++;
                digits++;
            }
        }
        if (digits==0) {
            return start;
        }
        if (index<value.length()&&(value.charAt(index)=='e'||value.charAt(index)=='E')) {
            int exponentStart=index;
            index++;
            if (index<value.length()&&(value.charAt(index)=='+'||value.charAt(index)=='-')) {
                index++;
            }
            int exponentDigits=0;
            while (index<value.length()&&isDigit(value.charAt(index))) {
                index++;
                exponentDigits++;
            }
            if (exponentDigits==0) {
                return exponentStart;
            }
        }
        return index;
    }
    private static double parseNumber(String value, int start, int end) throws SvgParseException {
        try {
            double number=Double.parseDouble(value.substring(start, end));
            if (!Double.isFinite(number)) {
                throw error("transform number must be finite");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new SvgParseException("invalid transform number", exception);
        }
    }
    private static int skipWhitespace(String value, int start) {
        int index=start;
        while (index<value.length()&&isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }
    private static int skipSeparators(String value, int start) {
        int index=skipWhitespace(value, start);
        if (index<value.length()&&value.charAt(index)==',') {
            index=skipWhitespace(value, index+1);
        }
        return index;
    }
    private static void requireCount(String name, int actual, int expected) throws SvgParseException {
        if (actual!=expected) {
            throw error(name+" expects "+expected+" arguments");
        }
    }
    private static boolean isWhitespace(char value) {
        return value==' '||value=='\t'||value=='\r'||value=='\n'||value=='\f';
    }
    private static boolean isDigit(char value) {
        return value>='0'&&value<='9';
    }
    private static boolean isTransformNameCharacter(char value) {
        return (value>='a'&&value<='z')||(value>='A'&&value<='Z');
    }
    private static void validateFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name+" must be finite");
        }
    }
    private static long bits(double value) {
        return Double.doubleToLongBits(value);
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
    private static SvgParseException error(String message) {
        return new SvgParseException(message);
    }
}
