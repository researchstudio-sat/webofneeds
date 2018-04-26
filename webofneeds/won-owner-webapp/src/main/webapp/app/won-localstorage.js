/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";

export function markUriAsRead(uri) {
    //TODO: BETTER IMPL
    if(!isUriRead(uri)){
        let readUrisString = window.localStorage.getItem(READ_URIS);
        if(!readUrisString){
            readUrisString = JSON.stringify([uri]);
        }else{
            try{
                let readUriList = JSON.parse(readUrisString);
                readUriList.push(uri);
                readUrisString = JSON.stringify(readUriList);
            }catch(e) {
                resetUrisRead();
                readUrisString = JSON.stringify([uri]);
            }
        }

        window.localStorage.setItem(READ_URIS, readUrisString);
    }
}

export function isUriRead(uri) {
    //TODO: BETTER IMPL
    let readUrisString = window.localStorage.getItem(READ_URIS);

    if(readUrisString) {
        let readUriList = JSON.parse(readUrisString);

        for(var i=0; i < readUriList.length; i++){
            if(readUriList[i] === uri) {
                return true;
            }
        }
    }
    return false;
}

export function resetUrisRead() {
    window.localStorage.removeItem(READ_URIS);
}
