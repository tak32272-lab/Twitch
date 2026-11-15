// MonsterStrike 33.1.0 Mod - Frida gadget script (libgadget.script.so)
// 全機能 + MonstSwitch連携(サーバー方式) + 診断ログ
"use strict";

var LIB = "libmonsterstrike.so";
var DMG = 99999999;
var LOGPATHS = [
    "/sdcard/Android/data/jp.co.mixi.monsterstrike/files/monst_log.txt",
    "/storage/emulated/0/Android/data/jp.co.mixi.monsterstrike/files/monst_log.txt",
    "/data/data/jp.co.mixi.monsterstrike/files/monst_log.txt"
];
var _fopen=null,_fputs=null,_fclose=null;
try {
    _fopen=new NativeFunction(Module.getExportByName("libc.so","fopen"),"pointer",["pointer","pointer"]);
    _fputs=new NativeFunction(Module.getExportByName("libc.so","fputs"),"int",["pointer","pointer"]);
    _fclose=new NativeFunction(Module.getExportByName("libc.so","fclose"),"int",["pointer"]);
} catch(e){}
function logw(msg){
    var line="["+Date.now()+"] "+msg+"\n";
    if(!_fopen) return;
    for(var i=0;i<LOGPATHS.length;i++){ try{ var fp=_fopen(Memory.allocUtf8String(LOGPATHS[i]),Memory.allocUtf8String("a")); if(!fp.isNull()){ _fputs(Memory.allocUtf8String(line),fp); _fclose(fp);} }catch(e){} }
}
logw("=== script loaded ===");

// 機能トグル(全ON)。MonstSwitchアプリから127.0.0.1:28900で上書きされる
var STATE = { onepunch:true, invin:true, ss:true, fantastic:true, critical:true, penetrate:true, heart:true, buff:true, barrier:true, zone:true };

function hookWhenReady(){
    var base=null;
    try{ base=Module.findBaseAddress(LIB); }catch(e){}
    if(!base){ try{ base=Process.getModuleByName(LIB).base; }catch(e){} }
    if(!base){ setTimeout(hookWhenReady,1000); return; }
    logw("base="+base);

    var ok=0,ng=0;
    function atLeave(off,fn){ try{ Interceptor.attach(base.add(off),{onLeave:fn}); ok++; }catch(e){ ng++; logw("FAIL L 0x"+off.toString(16)); } }
    function atEnter(off,fn){ try{ Interceptor.attach(base.add(off),{onEnter:fn}); ok++; }catch(e){ ng++; logw("FAIL E 0x"+off.toString(16)); } }
    function retVal(off,val,flag){ atLeave(off,function(r){ if(STATE[flag]) r.replace(ptr(val)); }); }

    // 検知バイパス(常時ON)
    [0x06b8f2a0,0x08447ee0,0x06c32b9c,0x04ab3d80,0x053f099c,0x04c6e234].forEach(function(o){ atLeave(o,function(r){ r.replace(ptr(0)); }); });

    // ワンパン
    [0x04a91d7c,0x0655d02c,0x04eb013c,0x05be57f8,0x051d73e8].forEach(function(o){ atEnter(o,function(a){ if(STATE.onepunch) this.context.x1=ptr(DMG); }); });
    atEnter(0x05080f6c,function(a){ if(STATE.onepunch){ try{a[2].writeFloat(99999999.0);}catch(e){} } });

    // 無敵
    retVal(0x04c15198,0,'invin');
    atEnter(0x061402d4,function(a){ if(STATE.invin){ try{a[2].writeU32(0);}catch(e){} } });
    atEnter(0x048606a8,function(a){ if(STATE.invin){ try{a[2].writeU32(0);}catch(e){} } });

    // SS無限(複数箇所を網羅)
    retVal(0x08083ea4,1,'ss');  // isStrikeShotExecuteInTurn→1(本命)
    retVal(0x05855d1c,0,'ss');  // getStrikeShotTurn→0
    atEnter(0x075c2f08,function(a){ if(STATE.ss) this.context.x1=ptr(1); });
    atLeave(0x075c2f08,function(r){ if(STATE.ss) r.replace(ptr(1)); });

    // Fantastic
    retVal(0x0574143c,1,'fantastic');
    retVal(0x060e40a0,1,'fantastic');

    // クリティカル
    retVal(0x05c611ac,0,'critical');
    retVal(0x0496310c,1,'critical');

    // 反射→貫通
    retVal(0x05a4b5d4,1,'penetrate');
    retVal(0x06ae6234,1,'penetrate');

    // ハート(回復)常時
    retVal(0x072cd744,100,'heart');

    // バフ(パワーアップ壁レベル)
    retVal(0x7fe11a8,9,'buff');

    // バリア無効化
    retVal(0x04e809b8,0,'barrier');

    // ゾーン無効化(フィールドギミック: 霧/ダメウォール/減速)
    retVal(0x06b0e7a4,0,'zone');                                  // FieldFog::checkHit→0
    atEnter(0x05166f9c,function(a){ if(STATE.zone){ this.skip=true; } }); // damageHitAtFireGround(効果は無敵で無害化済)
    atLeave(0x06f67400,function(r){ /* SpeedDownPoint hit: 効果なし */ });

    logw("hooks done: ok="+ok+" ng="+ng);
    startServer();
}

// === MonstSwitch連携: 28900でサーバー待受(アプリがクライアントで状態送信) ===
function startServer(){
    try {
        Socket.listen({ host:"127.0.0.1", port:28900, backlog:4 }).then(function(listener){
            logw("server listening 28900");
            function acceptLoop(){
                listener.accept().then(function(conn){
                    conn.input.read(512).then(function(buf){
                        try {
                            if(buf && buf.byteLength>0){
                                var u=new Uint8Array(buf),t=""; for(var i=0;i<u.length;i++)t+=String.fromCharCode(u[i]); t=t.trim();
                                if(t.length>0){
                                    ["onepunch","invin","ss","fantastic","critical","penetrate","heart","buff","barrier","zone"].forEach(function(k){
                                        if(t.indexOf(k+"=1")>=0) STATE[k]=true;
                                        else if(t.indexOf(k+"=0")>=0) STATE[k]=false;
                                    });
                                }
                            }
                        } catch(e){}
                        try{ conn.close(); }catch(e){}
                        acceptLoop();
                    }).catch(function(){ try{conn.close();}catch(e){} acceptLoop(); });
                }).catch(function(){ setTimeout(acceptLoop,500); });
            }
            acceptLoop();
        }).catch(function(e){ logw("listen fail: "+e); });
    } catch(e){ logw("server err: "+e); }
}

setTimeout(hookWhenReady,500);
