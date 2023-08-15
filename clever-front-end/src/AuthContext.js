import React, { createContext, useState } from "react";


export const AuthContext = createContext();

let adressMap = new Map([
  ["admin", "http://localhost:50008"],
  ["indoidja", "http://localhost:50005"],
  // ["bmriidja", "http://localhost:50007"],
  ["cenaidja", "http://localhost:50006"],
  ["ribsinmb", "http://localhost:50013"],
  ["masssgsg", "http://localhost:50009"],
  ["dbsssgsg", "http://localhost:50010"],
  ["hdfcinbb", "http://localhost:50014"],
  ["lpmanager", "http://localhost:50017"],
]);

// http://192.168.25.167:50006/me

adressMap = new Map([
  ["admin", "http://3.23.23.175:50008"],
  ["indoidja", "http://3.23.23.175:50005"],
  // ["bmriidja", "http://localhost:50007"],
  ["cenaidja", "http://3.23.23.175:50006"],
  ["ribsinmb", "http://3.23.23.175:50013"],
  ["masssgsg", "http://3.23.23.175:50009"],
  ["dbsssgsg", "http://3.23.23.175:50010"],
  ["hdfcinbb", "http://3.23.23.175:50014"],
  ["lpmanager", "http://3.23.23.175:50017"],
]);


// adressMap = new Map([
//   ["admin", "http://localhost:8080"],
//   ["indoidja", "http://localhost:8080"],
//   // ["bmriidja", "http://localhost:50007"],
//   ["cenaidja", "http://localhost:8080"],
//   ["ribsinmb", "http://localhost:8080"],
//   ["masssgsg", "http://localhost:8080"],
//   ["dbsssgsg", "http://localhost:8080"],
//   ["hdfcinbb", "http://localhost:8080"],
//   ["lpmanager", "http://localhost:8080"],
// ]);

// Here you would make a call to your authentication API
// and set the user state accordingly
export const AuthProvider = ({ children }) => {
  const usernameLS = localStorage.getItem("username");
  const [user, setUser] = useState((usernameLS !== "" | usernameLS !== null) ? { username: usernameLS } : null);
  let allowwedUsers = ["indoidja", "ribsinmb", "masssgsg","","cenaidja","dbsssgsg","hdfcinbb","lpmanager"];
  const login = (username, password) => {
    if (allowwedUsers.includes(username.toLowerCase())) {
      if (username.toLowerCase() === "indoidja" || username.toLowerCase() === "ribsinmb"  || username.toLowerCase() === "masssgsg") {
        localStorage.setItem("role", "central-bank");
      }
      if (username.toLowerCase() === "cenaidja" || username.toLowerCase() === "dbsssgsg"  || username.toLowerCase() === "hdfcinbb") {
        localStorage.setItem("role", "lqprovider");
      }
      if (username.toLowerCase() === "lpmanager") {
        localStorage.setItem("role", "lpmanager");
      }
      setUser({username});
      localStorage.setItem("username", username);
      localStorage.setItem("host", adressMap.get(localStorage.getItem("username")));
    } else {
      throw new Error("Pengguna tidak ditemukan")
    }
  };

  const logout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    setUser(null);
  };
  return (
    <AuthContext.Provider value={{ user, allowwedUsers, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};