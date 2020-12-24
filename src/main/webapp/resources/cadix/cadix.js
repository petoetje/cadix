
var cadixRoots = new WeakMap; // map DOM element to cadixRoot. When DOM element is deleted, map is cleaned

const NOELEM = "noelem";
const NOKEY = "nokey";
const NOELEMENTTYPE = "CadixForgotReactElementTypeAttribute";

class CadixEntry {
    constructor() {
        this.children = new Array(); // ids
        this.oldChildren = new Array(); //children of previous run
        this.reactProps = new Object();
        this.reactElement = NOELEM;
        this.reactElementType = NOELEMENTTYPE;
        this.reactKey = NOKEY;


    }

}

class CadixTree {
    constructor() {
        //by using a weakmap, with DOM bases keys, we are sure entries are removed in time
        this.cadixMap = new Map(); //  keys are JSF id ,
        //                                 values are CadixEntries which are children of other
        // elements.  Execpt root, which is kept
        //explicitly in another field
        this.cadixRoot = null; // CadixEntry, to keep weak reference from disappearing
    }

}



function cadixCreateComp(myId, parentId, rootId, props, reactElementType) {
    console.log("Cadix myId:" + myId + " parent:" + parentId + " root:" + rootId + " props:" + props);

    //if I am root, I need to create the React mount point and Cadix map, if not yet done
    if (rootId === myId) {

        console.log("Cadix check root");
        var cadixRootId = rootId + "-cadix";
        var parentNode = document.getElementById(parentId);
        var cadixRootElement = document.getElementById(cadixRootId);
        if (!cadixRootElement) {
            console.log("Cadix create root : " + cadixRootId);
            //create the React mount point
            cadixRootElement = document.createElement("span");
            // first append, then set id
            parentNode.appendChild(cadixRootElement);
            cadixRootElement.setAttribute("id", cadixRootId);
            //when we create a new Dom element cadixElement
            //we store it in cadixRoots
            var cadixTree = new CadixTree();
            var cadixEntry = new CadixEntry();
            cadixEntry.reactKey = myId;
            cadixTree.cadixRoot = cadixEntry;
            //also store in Map (because children look me up)
            cadixTree.cadixMap.set(myId, cadixEntry);
            //TODO : properties
            //where to store cadixTree...
            cadixRoots.set(cadixRootElement, cadixTree);
        }
    }

    // at this point root exists and is initialized
    //following step is done for each element, including root
    var cadixRootElement = document.getElementById(rootId + "-cadix");
    var cadixTree = cadixRoots.get(cadixRootElement);

    var cadixEntry = cadixTree.cadixMap.get(myId);
    if (!cadixEntry) {
        console.log("New cadix entry for " + myId);
        cadixEntry = new CadixEntry();
        cadixEntry.reactKey = myId;
        cadixTree.cadixMap.set(myId, cadixEntry);
    }

    if (props !== null) {
        cadixEntry.reactProps = JSON.parse(props);
    }
    cadixEntry.reactElementType = reactElementType;

    var cadixParentEntry = cadixTree.cadixMap.get(parentId);

    cadixParentEntry.children.push(myId);


    //also add our DOM input element (which is used by JSF) 
    //you can find the Form from there by element.form;
    cadixEntry.reactProps.jsfinput = document.getElementById(myId + "-input");





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

    //purge non-rendered children


    if (cadixEntry.reactElement === NOELEM) {
        //create react component
        createReactElement(cadixEntry, cadixTree);

        //if root is created, then render !
        if (myId === rootId) {
            console.log("Render time");
            ReactDOM.render(cadixEntry.reactElement, cadixRootElement);
        } else {
            //if I am created, force re-creating my parent
            var parentCadixEntry = cadixTree.cadixMap.get(parentId);
            if (parentCadixEntry && parentCadixEntry.reactElement !== NOELEM) {
                console.log("Destroying parent " + parentId);
                parentCadixEntry.reactElement = null;
            }
        }
    } else {
        //otherwise just setProps
        console.log("just set props id:" + myId);
        cadixEntry.reactElement.props = cadixEntry.reactElement.props;

    }
}

//create the React component for the cadixEntry
function createReactElement(cadixEntry, cadixTree) {
    console.log("createReactElement cadixEntry:" + cadixEntry + " /// cadixMap:" + cadixTree.cadixMap);
    //when this function is called, all children will aready have a React comp,
    //because the same function was called by JSF before, in order children before parents
    var reactChildren = new Array();
    if (cadixEntry.children) {
        //attn : first value, then key
        cadixEntry.children.forEach((ce) => {
            childCadixEntry = cadixTree.cadixMap.get(ce);
            console.log("child:" + ce);
            if (childCadixEntry.reactElementType === "_noncadix") {
                //use literal text (non React object)
                reactChildren.push(childCadixEntry.reactProps.text);
            } else {
                reactChildren.push(childCadixEntry.reactElement);
            }
        });
    }
    //when we arrive here, we assume children aleady have react Ids
    //make sure key is set
    cadixEntry.reactProps.key = cadixEntry.reactKey;

    cadixEntry.reactElement = React.createElement(cadixEntry.reactElementType, cadixEntry.reactProps, reactChildren);

}

