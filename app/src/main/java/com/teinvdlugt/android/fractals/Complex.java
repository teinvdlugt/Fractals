package com.teinvdlugt.android.fractals;

public class Complex {

    private double real, imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public static Complex multiply(Complex c1, Complex c2) {
        double real = c1.real * c2.real - c1.imaginary * c2.imaginary;
        double img = c1.real * c2.imaginary + c1.imaginary * c2.real;
        return new Complex(real, img);
    }

    public static Complex add(Complex c1, Complex c2) {
        return new Complex(c1.real + c2.real, c1.imaginary + c2.imaginary);
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
