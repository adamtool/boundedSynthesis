package uniolunisaar.adam.bounded.qbfapproach.unfolder;

public class Triple<A, B, C> {

    public final A a;
    public final B b;
    public final C c;

    public Triple(A first, B second, C third) {
        this.a = first;
        this.b = second;
        this.c = third;
    }
}