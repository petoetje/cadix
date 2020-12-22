
var cadixRoots = new WeakMap; // map DOM element to cadixRoot. When DOM element is deleted, map is cleaned


class CadixEntry {
    constructor() {
        this.children = new Array();//ids
        this.props = new Object();
        this.reactElement = null;
        this.oldchildren = new Array(); //ids of previous render
    }

}

class CadixTree {
    constructor() {
        this.cadixMap = new Map(); //  keys are CadixEntries, which are children of other
        // elements.  Execpt root, which is kept
        //explicitly in another field
        this.cadixRoot = null; // CadixEntry, to keep weak reference from disappearing
    }

}



function cadixCreateComp(myId, parentId, rootId) {
    console.log("Cadix myId:" + myId + " parent:" + parentId + " root:" + rootId);

    //if I am root, I need to create the React mount point and Cadix map, if not yet done
    if (rootId === myId) {

        console.log("Cadix check root");
        var cadixId = myId + "-cadix";
        var parentNode = document.getElementById(parentId);
        var cadixElement = document.getElementById(cadixId);
        if (!cadixElement) {
            console.log("Cadix create root : " + cadixId);
            //create the React mount point
            cadixElement = document.createElement("span");
            // first append, then set id
            parentNode.appendChild(cadixElement);
            cadixElement.setAttribute("id", cadixId);
            //when we create a new Dom element cadixElement
            //we store it in cadixRoots
            var cadixTree = new CadixTree();
            var cadixEntry = new CadixEntry();

            cadixTree.cadixRoot = cadixEntry;
            //also store in Map (because children look me up)
            cadixTree.cadixMap.set(myId, CadixEntry);
            //TODO : properties
            //where to store cadixTree...
            cadixRoots.set(cadixElement, cadixTree);
        }
    }

    // at this point root exists and is initialized
    //following step is done for each element, including root
    var cadixRootElement = document.getElementById(rootId + "-cadix");
    var cadixTree = cadixRoots.get(cadixRootElement);

    var cadixEntry = cadixTree.cadixMap.get(myId);
    if (cadixEntry) {
        //keep track of old children, to be able to remove them
        cadixEntry.oldchildren = Array.from(cadixEntry.children);
        cadixEntry.children = new Array();

    } else {
        cadixEntry = new CadixEntry();
        cadixTree.cadixMap.set(myId, cadixEntry);
    }

    //cadixEntry.props = ...;

    //if parent is in map,a dd myself to children
    if (cadixTree.cadixMap.has(parentId)) {
        cadixTree.cadixMap.get(parentId).children.push(myId);
    }



}

//compnentEnd fun
//not exist -> createElement
//same children -> set properties
// more/fewer children -> purge and replace with createElement
//set properties or createElement 
//check dangling, trigger rerender if set of childs different
function cadixActivateComp(myId, parentId, rootId) {
    console.log("Cadix Activate myId:" + myId + " parent:" + parentId + " root:" + rootId);
    //check if already a React component
    // at this point root exists and is initialized
    //following step is done for each element, including root
    var cadixRootElement = document.getElementById(rootId + "-cadix");
    var cadixTree = cadixRoots.get(cadixRootElement);
    var cadixEntry = cadixTree.cadixMap.get(myId);
    var wasCreated = false;
    if (cadixEntry.reactElement === null) {
        //create react component
        createReactElement(cadixEntry);
        wasCreated = true;
    } else {
        //compare children
    }
    //if root is created, then render !
    if (myId === rootId && wasCreated) {
        ReactDOM.render(cadixEntry.reactElement, cadixRootElement);
    }

}

//create the React component for the cadixEntry
function createReactElement(cadixEntry) {
    //when this function is called, all children will aready have a React comp,
    //because the same function was called by JSF before, in order children before parents
    var reactChildren = array();
    if (cadixEntry.children) {
        cadixEntry.children.forEach(ce => reactChildren.push(ce.reactElement));
    }
    //when we arrive here, we assume children aleady have react Ids
    cadixEntry.reactElement = React.createElement(div, null, reactChildren);
}