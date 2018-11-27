
import java.util.ArrayList;




public class QTreeNode {
    String name;
    private Point upperLeftPoint;
    private Point lowerRightPoint;
    private double distancePerPixel;
    ArrayList<QTreeNode> children = new ArrayList<QTreeNode>();
    QTreeNode parent = null;

    public QTreeNode() {
        name = "root";
        upperLeftPoint = new Point(-122.2998046875, 37.892195547244356);
        lowerRightPoint = new Point(-122.2119140625, 37.82280243352756);
        distancePerPixel = (lowerRightPoint.getLonX() - upperLeftPoint.getLonX()) / 256;
    }

    public QTreeNode(String name, QTreeNode parent) {
        this.name = name;
        this.parent = parent;
        upperLeftPoint = new Point(calcUllon(name), calcUllat(name));
        lowerRightPoint = new Point(calcLrlon(name), calcLrlat(name));
        distancePerPixel = (lowerRightPoint.getLonX() - upperLeftPoint.getLonX()) / 256;
    }

    public double getDistancePerPixel() {
        return distancePerPixel;
    }

    public double calcUllat(String x) {
        String[] idList = name.split("");
        if (idList[name.length() - 1].equals("1")) {
            return parent.upperLeftPoint.getLatY();
        } else if (idList[name.length() - 1].equals("2")) {
            return parent.upperLeftPoint.getLatY();
        } else if (idList[name.length() - 1].equals("3")) {
            return parent.upperLeftPoint.getLatY()
                    + ((parent.lowerRightPoint.getLatY() - parent.upperLeftPoint.getLatY()) / 2);
        } else {
            return parent.upperLeftPoint.getLatY()
                    + ((parent.lowerRightPoint.getLatY() - parent.upperLeftPoint.getLatY()) / 2);
        }
    }
    public double calcUllon(String xxx) {
        String[] idList = name.split("");
        if (idList[name.length() - 1].equals("1")) {
            return parent.upperLeftPoint.getLonX();
        } else if (idList[name.length() - 1].equals("2")) {
            return parent.lowerRightPoint.getLonX()
                    + ((parent.upperLeftPoint.getLonX() - parent.lowerRightPoint.getLonX()) / 2);
        } else if (idList[name.length() - 1].equals("3")) {
            return parent.upperLeftPoint.getLonX();
        } else {
            return parent.lowerRightPoint.getLonX()
                    + ((parent.upperLeftPoint.getLonX() - parent.lowerRightPoint.getLonX()) / 2);
        }
    }
    public double calcLrlat(String yyy) {
        String [] idList = name.split("");
        if (idList[name.length() - 1].equals("1")) {
            return parent.upperLeftPoint.getLatY()
                    + ((parent.lowerRightPoint.getLatY() - parent.upperLeftPoint.getLatY()) / 2);
        } else if (idList[name.length() - 1].equals("2")) {
            return parent.upperLeftPoint.getLatY()
                    + ((parent.lowerRightPoint.getLatY() - parent.upperLeftPoint.getLatY()) / 2);
        } else if (idList[name.length() - 1].equals("3")) {
            return parent.lowerRightPoint.getLatY();
        } else {
            return parent.lowerRightPoint.getLatY();
        }
    }
    public double calcLrlon(String xyxy) {
        String[] idList = name.split("");
        if (idList[name.length() - 1].equals("1")) {
            return parent.lowerRightPoint.getLonX()
                    + ((parent.upperLeftPoint.getLonX() - parent.lowerRightPoint.getLonX()) / 2);
        } else if (idList[name.length() - 1].equals("2")) {
            return parent.lowerRightPoint.getLonX();
        } else if (idList[name.length() - 1].equals("3")) {
            return parent.lowerRightPoint.getLonX()
                    + ((parent.upperLeftPoint.getLonX() - parent.lowerRightPoint.getLonX()) / 2);
        } else {
            return parent.lowerRightPoint.getLonX();
        }
    }

    public boolean isLeaf() {
        return this.name.length() == 7;
    }

    public void addChildren() {
        if (name.equals("root")) {
            children.add(new QTreeNode("1", this));
            children.add(new QTreeNode("2", this));
            children.add(new QTreeNode("3", this));
            children.add(new QTreeNode("4", this));
            return;
        }
        children.add(new QTreeNode(name + "1", this));
        children.add(new QTreeNode(name + "2", this));
        children.add(new QTreeNode(name + "3", this));
        children.add(new QTreeNode(name + "4", this));
    }

    public ArrayList<QTreeNode> getChildren() {
        return children;
    }

    public Point getUpperLeftPoint() {
        return upperLeftPoint;
    }

    public Point getLowerRightPoint() {
        return lowerRightPoint;
    }

    public String getName() {
        return name;
    }

    public boolean possibleTile(double ullat, double ullon, double lrlon, double lrlat) {
        if (ullon >= this.getLowerRightPoint().getLonX()) {
            return false;
        } else if (ullat <= this.getLowerRightPoint().getLatY()) {
            return false;
        } else if (lrlon <= this.getUpperLeftPoint().getLonX()) {
            return false;
        } else if (lrlat >= this.getUpperLeftPoint().getLatY()) {
            return false;
        }
        return true;
    }

    public QTreeNode compare(QTreeNode t) {
        if (this.getUpperLeftPoint().getLatY() > t.getUpperLeftPoint().getLatY()) {
            return this;
        } else if (this.getUpperLeftPoint().getLatY() == t.getUpperLeftPoint().getLatY()) {
            if (this.upperLeftPoint.getLonX() < t.upperLeftPoint.getLonX()) {
                return this;
            } else {
                return t;
            }
        } else {
            return t;
        }
    }

    public void searchHelper(double queryDPP, double ullat, double ullon,
                             double lrlon, double lrlat,
                             ArrayList<QTreeNode> rtn) {
        if ((this.possibleTile(ullat, ullon, lrlon, lrlat)
                && this.getDistancePerPixel() <= queryDPP) || (this.isLeaf()
                && this.possibleTile(ullat, ullon, lrlon, lrlat))) {

            rtn.add(this);
        } else if (this.possibleTile(ullat, ullon, lrlon, lrlat)) {
            this.addChildren();
            for (QTreeNode c : this.getChildren()) {
                c.searchHelper(queryDPP, ullat, ullon, lrlon, lrlat, rtn);
            }
        } else {
            return;
        }
    }
}

