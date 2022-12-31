
var cadixRoots = new WeakMap; // map DOM element to cadixRoot. When DOM element is deleted, map is cleaned

const NOELEM = "noelem";
const NOKEY = "nokey";
const NOELEMENTTYPE = "cadixForgotReactElementTypeAttribute"; //start with lower case, catches more React erros


class CadixEntry {
    constructor() {
        this.children = new Array(); // ids
        this.oldChildren = new Array(); //children of previous run
        this.reactProps = new Object();
        this.reactElement = NOELEM;
        this.reactElementType = NOELEMENTTYPE;
        this.reactKey = NOKEY;
        this.updateFun = null; //function to udate CadixWrapperState (and re-render wrapped comp)
        this._jsf = Object(); //will contain JSF interop functions
    }

    get jsf() {
        return this._jsf;
    }

    get react() {
        return this.reactElement;
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
        this.reactRoot = null; //as created by ReactDOM.createRoot

    }

}




function registerUpdateFunction(cadixTree, jsfId, fun) {
    cadixTree.cadixMap.get(jsfId).updateFun = fun;
}

// wrapper arounf React component, to be able to re-render it with new prop or children
function CadixReactWrapper(props) {
// React.createElement(cadixEntry.reactElementType, cadixEntry.reactProps, reactChildren);
    const [fullState, setFullState] = React.useState(props);
    props.registerUpdateFun(props.cadixTree, props.cadixEntry.reactKey, setFullState);
    newKey = props.cadixEntry.reactKey + "X";
    return React.createElement(fullState.cadixEntry.reactElementType, {...fullState.cadixEntry.reactProps, key: newKey}, fullState.reactChildren);
}

function cadixCreateComp(myId, parentId, rootId, props, reactElementType, innerHtml, execute, render) {
    console.log("Cadix myId:" + myId + " parent:" + parentId + " root:" + rootId + " props:" + props + " elementType:" + reactElementType);
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
        //replace magic values
        for (const[key, value] of Object.entries(cadixEntry.reactProps)) {
            console.log("Checking prop:" + key + "  value:" + value);
            if (key.startsWith("p_")) {
                var newKey = key.substr(2); //remove p_
                cadixEntry.reactProps[newKey] = value;
                delete cadixEntry.reactProps[key];
            }  else if (key.startsWith("a_")) {
                var newKey = key.substr(2); //remove a_
                var splitpos = value.indexOf('|');
                var split = [value.slice(0, splitpos), value.slice(splitpos + 1)];
                var tag = split[0];
                //split again
                splitpos = split[1].indexOf('|');
                split = [split[1].slice(0, splitpos), split[1].slice(splitpos + 1)];
                var args = split[0];
                var funcdef = split[1];
                console.log("funcdef:" + funcdef);
                var func = new Function(args, funcdef);
                cadixEntry.reactProps[newKey] = generateTriggerCadixEvent(myId, cadixEntry, tag, func, execute, render);
                delete cadixEntry.reactProps[key];
            } else if (key.startsWith("f_")) {
                var newKey = key.substr(2); //remove f_
                var splitpos = value.indexOf('|');
                var split = [value.slice(0, splitpos), value.slice(splitpos + 1)];
                var args = split[0];
                var funcdef = split[1];
                cadixEntry.reactProps[newKey] = function () {
                    var inner = new Function(args, funcdef);
                    return inner.apply(cadixEntry, arguments);
                };
                delete cadixEntry.reactProps[key];
            }
        }
    }
    console.log("corrected props:" + JSON.stringify(cadixEntry.reactProps));
    if (innerHtml !== null) {
        cadixEntry.reactProps.dangerouslySetInnerHTML = {__html: innerHtml};
    }



    cadixEntry.reactElementType = reactElementType;
    ///keep copy; to compate later
    cadixEntry.oldChildren = cadixEntry.children;
    cadixEntry.children = new Array();
    //update children list of parent.  This way we can detect orphans
    if (rootId !== myId) {
        var cadixParentEntry = cadixTree.cadixMap.get(parentId);
        if (!cadixParentEntry.children.includes(myId)) {
            cadixParentEntry.children.push(myId);
        }
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
    //purge non-rendered children
    cadixEntry.oldChildren.forEach((oc) => {
        if (!cadixEntry.children.includes(oc)) {
            cadixTree.cadixMap.delete(oc);
        }
    });
    if (cadixEntry.reactElement === NOELEM) {
//create react component
        createReactElement(myId, cadixEntry, cadixTree);
        //if root is created, then render !
        if (myId === rootId) {
            console.log("Render time");
            const root = ReactDOM.createRoot(cadixRootElement);
            cadixTree.reactRoot = root;
            root.render(cadixEntry.reactElement);
        } else {
//if I am created, force re-creating my parent
            var parentCadixEntry = cadixTree.cadixMap.get(parentId);
            if (parentCadixEntry && parentCadixEntry.reactElement !== NOELEM) {
                console.log("Destroying parent " + parentId);
                parentCadixEntry.reactElement = null;
            }
        }
    } else {

//otherwise force a re-render with updated props
        console.log("just set props id:" + myId);
        //cadixEntry.reactElement.props = cadixEntry.reactProps;   
        // createReactElement(myId, cadixEntry, cadixTree);
        // var parentCadixEntry = cadixTree.cadixMap.get(parentId);
        //createReactElement(parentId, parentCadixEntry, cadixTree);
        //parentCadixEntry.reactElement.props =  parentCadixEntry.reactElement.props;
        //cadixEntry.reactElement.forceUpdate();
        //cadixTree.reactRoot.render(cadixTree.cadixRoot.reactElement);
        createReactElement(myId, cadixEntry, cadixTree);
        //update children list of parent.  This way we can detect orphans



    }
}

//create the React component for the cadixEntry
function createReactElement(myId, cadixEntry, cadixTree) {
    console.log("createReactElement cadixEntry:" + cadixEntry + " /// cadixMap:" + cadixTree.cadixMap);
    //when this function is called, all children will aready have a React comp,
    //because the same function was called by JSF before, in order children before parents
    var reactChildren = null;
    if (cadixEntry.children && cadixEntry.children.length > 0) {
        reactChildren = new Array();
        //attn : first value, then key
        cadixEntry.children.forEach((ce) => {
            childCadixEntry = cadixTree.cadixMap.get(ce);
            console.log("child:" + ce);
            reactChildren.push(childCadixEntry.reactElement);
            /* if (childCadixEntry.reactElementType === "_noncadix") {
             //use literal text (non React object)
             reactChildren.push(childCadixEntry.reactProps.text);
             } else {
             reactChildren.push(childCadixEntry.reactElement);
             }*/
        });
    }
//when we arrive here, we assume children aleady have react Ids
//make sure key is set
    cadixEntry.reactProps.key = cadixEntry.reactKey;
    console.log("Create args: entry:" + JSON.stringify(JSON.decycle(cadixEntry)));
    if (cadixEntry.reactElement === NOELEM) {

// cadixEntry.reactElement = React.createElement(cadixEntry.reactElementType, cadixEntry.reactProps, reactChildren);
        cadixEntry.reactElement = React.createElement(CadixReactWrapper, {"key": cadixEntry.reactProps.key, "cadixTree": cadixTree, "cadixEntry": cadixEntry, "reactChildren": reactChildren, "registerUpdateFun": registerUpdateFunction});
        //cadixEntry.reactElement = CadixReactWrapper(cadixEntry, reactChildren);

        //also add our DOM input element (which is used by JSF) 
        //you can find the Form from there by element.form;
        //cadixEntry.reactProps.jsfInput = document.getElementById(myId + "-input");
        //only do this for real React components
        if (typeof cadixEntry.reactElementType === "object") {
            console.log("adding get/set value");
            cadixEntry._jsf.setValue = function (value) {
                document.getElementById(myId + "-input").value = value;
            };
            cadixEntry._jsf.getValue = function () {
                return document.getElementById(myId + "-input").value;
            };
            cadixEntry._jsf.submitForm = function () {
                document.getElementById(myId + "-input").form.submit();
            };
            console.log(Object.keys(cadixEntry.reactElement));
        }
    } else {
        cadixEntry.updateFun({"key": cadixEntry.reactProps.key, "cadixTree": cadixTree, "cadixEntry": cadixEntry, "reactChildren": reactChildren, "registerUpdateFun": registerUpdateFunction});
    }


}

//generate a function that fires a CadixEvent in the backend
function generateTriggerCadixEvent(myId, cadixEntry, tag, func, execute, render) {
    return function () {
        var o = {};
        o['javax.faces.behavior.event'] = 'action';
        o.execute = execute;
        o.render = render;
        o['org.cadix.tag'] = tag;
        o['org.cadix.output'] = JSON.stringify(JSON.decycle(func.apply(cadixEntry, arguments)));
        faces.ajax.request(myId, null, o);
    };
}

