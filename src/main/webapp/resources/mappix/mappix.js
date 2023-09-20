//add JSON.stringify support for Holder
function mappxReplacer(key, value) {
    if (value instanceof Holder) {
        switch (value.holderType) {
            case HolderType.list:
                return Array.from(value.data.values()); // or with spread: value: [...value]
                break;
            default:
                return Array.from(value.data).reduce((obj, [key, value]) => {
                    obj[key] = value;
                    return obj;
                }, {});

        }
    } else if (value instanceof Map) {
        return Array.from(value).reduce((obj, [key, value]) => {
            obj[key] = value;
            return obj;
        }, {});

    } else {
        return value;
    }
}


const HolderType = {
    map: 'Map',
    list: 'List'
};

class Holder {
    constructor(holderType) {
        this.#data = new Map();
        this.#holderType = holderType;
    }
    #holderType = null;
    get holderType() {
        return this.#holderType;
    }
    set holderType(ht) {
        this.#holderType = ht;
    }
    #data
    get data() {
        return this.#data;
    }
    set data(d) {
        this.#data = d;
    }

}



var roots = new Holder();

//return the added Holder
//if a holder already exists at indexArray,
//then the current holder is returned
function addHolder(indexArray, holderType) {
    let curr = roots;
    let i = 0;
    while (i < indexArray.length) {
        let index = indexArray[i];
        let m = null;
        //never add a holder when holderTYpe not set
        if (!curr.data.has(index) && holderType !== null) {
            m = new Holder();
            if (i < indexArray.length - 1) {
                //intermediate must be map
                m.holderType = HolderType.map;
            } else {
                m.holderType = holderType;
            }
            curr.data.set(index, m);
        }
        curr = curr.data.get(index);
        i++;
    }
    return curr;
}

function addValue(indexArray, value) {
    let p = indexArray.slice(0, -1);
    let holder = addHolder(p);
    holder.data.set(indexArray.slice(-1)[0], value);
    return holder;
}

function addValueById(id, value) {
    return addValue(id.split(":"), value);
}

function getHolder(indexArray) {
    return addHolder(indexArray, null);
}

//console.log(JSON.stringify(roots, mappxReplacer));

if (typeof mappixValues === 'undefined') {
    mappixValues = {};
} 

function mappixGet(key) {
    return typeof mappixValues[key] === 'undefined' ? null : mappixValues[key];
}

function mappixSet(key,value) {
    mappixValues[key] = value;
}