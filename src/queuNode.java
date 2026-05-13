import java.util.Objects;

public class queuNode {
    public Node node;
    boolean fromParent;
    queuNode(Node n, boolean fromParent){
        node=n;
        this.fromParent=fromParent;
    }

    public Node getNode(){return node;}
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof queuNode)) return false;
        queuNode other = (queuNode) o;
        return this.fromParent == other.fromParent && this.node.getName().equals(other.node.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(node.getName(), fromParent);
    }
}
