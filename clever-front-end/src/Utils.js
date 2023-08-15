// export const getUsername = () => {
//     return localStorage.getItem("username");
// };
import Toastify from 'toastify-js'
import "toastify-js/src/toastify.css"

const queue = require('fastq')(worker, 1)

queue.push(42, function (err, result) {
  if (err) { throw err }
  console.log('the result is', result)
})

function worker (arg, cb) {
  cb(null, arg * 2)
}

const getHostAdress = () => {
    const adressMap = new Map([
        ["admin", "http://localhost:8080"],
        ["indoidja", "http://localhost:8080"],
        ["bmridja", "http://localhost:8080"],
        ["cnaidja", "http://localhost:8080"],
    ]);
    return adressMap.get(localStorage.getItem("username"));
};

export default function showAlert (text, status) {
    let color = "blue";
    if (status === "error") {
        color = "red";
    }
    if (status === "warning") {
        color = "orange";
    }
    if (status === "success") {
        color = "green";
    }
    Toastify({
        text: text ? text : "undefined info / message",
        close: true,
        duration: 5000,
        style: {
            background: color,
        }
    }).showToast();
};

export const  formatRupiah = (angka, prefix) => {
    var number_string = angka.toString().replace(/[^,\d]/g, "").toString(),
      split = number_string.split(","),
      sisa = split[0].length % 3,
      rupiah = split[0].substr(0, sisa),
      ribuan = split[0].substr(sisa).match(/\d{3}/gi);
  
    // tambahkan titik jika yang di input sudah menjadi angka ribuan
    if (ribuan) {
      const separator = sisa ? "." : "";
      rupiah += separator + ribuan.join(".");
    }
    rupiah = split[1] !== undefined ? rupiah + "," + split[1] : rupiah;
    return prefix === undefined ? rupiah : rupiah ? rupiah : "";
  }

 function intlFormat(num) {
    return new Intl.NumberFormat().format(Math.round(num*10)/10);
}
  
export const abbreviateNumber = (value) => {
//    let num = Math.floor(value.toString());
//    if(num >= 1000000000)
//        return formatRupiah(intlFormat(num/1000000000))+' B';
//    if(num >= 1000000)
//        return formatRupiah(intlFormat(num/1000000))+' M';
//    // if(num >= 1000)
    //     return formatRupiah(intlFormat(num/1000))+'k';
    return formatRupiah(value);
       // return formatRupiah(intlFormat(num));

}

export const showAjaxLoader = () => {
// we need to wait for ajax calls to be finished
var intervalID = window.setInterval(
    function() {
        if(window.Ajax.activeRequestCount === 0) {
            window.clearInterval(intervalID);
            return ('No active ajax calls');
        } else {
            return ('There are ' + window.Ajax.activeRequestCount + ' active ajax requests');
        }
}, 500);
}

export {getHostAdress}