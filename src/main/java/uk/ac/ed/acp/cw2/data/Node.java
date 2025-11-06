package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

// Data model to represent a node in a star
@Getter
@Setter
public class Node {
    Coordinate p;
    int g;          // num of steps moved
    int f;          // g + h
    Node parent;
    Node(Coordinate p, int g, int f, Node parent){
        this.p=p;
        this.g=g;
        this.f=f;
        this.parent=parent;
    }
}
