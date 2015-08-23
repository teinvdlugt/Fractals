package com.teinvdlugt.android.fractals;

public class Complex {

    private double real, imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public static Complex multiply(Complex c1, Complex c2) {
        return c1.multiply(c2);
    }

    public Complex multiply(Complex other) {
        double real = this.real * other.real - this.imaginary * other.imaginary;
        double img = this.real * other.imaginary + this.imaginary * other.real;
        return new Complex(real, img);
    }

    public static Complex add(Complex c1, Complex c2) {
        return c1.add(c2);
    }

    public Complex add(Complex other) {
        return new Complex(this.real + other.real, this.imaginary + other.imaginary);
    }

    public double radiusSquared() {
        return real * real + imaginary * imaginary;
    }

    public Complex copy() {
        return new Complex(real, imaginary);
    }

    public double getReal() {
        return real;
    }

    public void setReal(double real) {
        this.real = real;
    }

    public double getImaginary() {
        return imaginary;
    }

    public void setImaginary(double imaginary) {
        this.imaginary = imaginary;
    }
}
