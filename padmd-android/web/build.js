// Transpile padMD's main script so older Android WebViews (Chrome 49+) can run it.
const fs=require('fs'),babel=require('@babel/core');
const [src,dst]=process.argv.slice(2);let h=fs.readFileSync(src,'utf8');
const open="<script>\n'use strict';",i=h.indexOf(open),j=h.indexOf('</script>',i);
const code=h.slice(i+8,j);
const outc=babel.transformSync(code,{presets:[['@babel/preset-env',{targets:{chrome:'49'},modules:false,bugfixes:true}]],compact:false,comments:false}).code;
h=h.slice(0,i+8)+'\n'+outc+'\n'+h.slice(j);fs.writeFileSync(dst,h);console.log('ok',code.length,outc.length);
