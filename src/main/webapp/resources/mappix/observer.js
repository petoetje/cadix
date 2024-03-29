var Observer=function(){function l(d,b){return b.priority-d.priority}function d(d){return void 0===d?!0:null===d}var f;f=function b(){if(!(this instanceof b))return new b;this._channelSubscribers={}};f.prototype.subscribe=function(b,f,a){var c=this._channelSubscribers[b];d(c)&&(c=[]);if(d(a)){a=c;var e,h,g=0;if(!d(a))for(h=a.length,e=0;e<h;e++)0>a[e].priority&&a[e].priority<g&&(g=a[e].priority);a=g-1}c.push({priority:a,callback:f});c=c.sort(l);this._channelSubscribers[b]=c};f.prototype.unsubscribe=
function(b,f){var a,c=this._channelSubscribers[b];if(!d(c)){for(a=0;a<c.length;a++)if(c[a].callback===f){c.splice(a,1);break}0===c.length&&delete this._channelSubscribers[b]}};f.prototype.publish=function(b,f,a,c){var e,h,g,k=null;if(0===arguments.length||d(b))throw Error("channelId argument required.");1<arguments.length&&(k=Array.prototype.slice.call(arguments,1));g=this._channelSubscribers[b];h={channelId:b,cancel:!1};if(d(g))return!0;for(e=0;e<g.length;e++)if(g[e].callback.apply(h,k),h.cancel)return!1;
return!0};return f}(); //@ sourceMappingURL=./Observer-1.0.2.min.js.map
//define a global instance
if (typeof wobs === 'undefined') {
    wobs=new Observer();
} 