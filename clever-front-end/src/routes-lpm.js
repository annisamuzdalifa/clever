import React from "react";

import { Icon } from "@chakra-ui/react";
import {
  MdBarChart,
  // MdPerson,
  MdHome,
  MdLock,
  // MdOutlineShoppingCart,
  MdShuffleOn,
  MdCurrencyExchange,
  MdSend,
  MdDeleteSweep,
  MdVolunteerActivism,
  MdWidgets,
  
  
} from "react-icons/md";

// Admin Imports
import MainDashboard from "views/admin/default";
// import Buyer from "views/admin/buyer";
import Transfer from "views/admin/transfer";
import Issuance from "views/admin/issuance";
import History from "views/admin/history";
import Exchange from "views/admin/exchange";
import Redemption from "views/admin/redemption";



// import NFTMarketplace from "views/admin/marketplace";
// import Profile from "views/admin/profile";
// import DataTables from "views/admin/dataTables";
// import RTL from "views/admin/rtl";

// Auth Imports
import SignInCentered from "views/auth/signIn";
import WithdrawStake from "views/admin/withdraw-stake";
import WithdrawFee from "views/admin/withdraw-fee";


const routes = [
  {
    name: "Main Dashboard",
    layout: "/admin",
    path: "/default",
    icon: <Icon as={MdHome} width='20px' height='20px' color='inherit' />,
    component: MainDashboard,
  },
  {
    name: "History",
    layout: "/admin",
    path: "/history",
    icon: <Icon as={MdBarChart} width='20px' height='20px' color='inherit' />,
    component: History,
  },
  {
    name: "Exchange",
    layout: "/admin",
    path: "/exchange",
    icon: <Icon as={MdCurrencyExchange} width='20px' height='20px' color='inherit' />,
    component: Exchange,
  },
  {
    name: "Transfer",
    layout: "/admin",
    path: "/transfer",
    icon: <Icon as={MdSend} width='20px' height='20px' color='inherit' />,
    component: Transfer,
  },
  // {
  //   name: "Issuance",
  //   layout: "/admin",
  //   path: "/issuance",
  //   icon: <Icon as={MdShuffleOn} width='20px' height='20px' color='inherit' />,
  //   component: Issuance,
  // },
  {
    name: "Redemption",
    layout: "/admin",
    path: "/redemption",
    icon: <Icon as={MdDeleteSweep} width='20px' height='20px' color='inherit' />,
    component: Redemption,
  },
  // {
  //   name: "Withdraw Staking",
  //   layout: "/admin",
  //   path: "/redeem-stake",
  //   icon: <Icon as={MdWidgets} width='20px' height='20px' color='inherit' />,
  //   component: WithdrawStake,
  // },
  // {
  //   name: "Withdraw Bonus",
  //   layout: "/admin",
  //   path: "/redeem-fee",
  //   icon: <Icon as={MdVolunteerActivism} width='20px' height='20px' color='inherit' />,
  //   component: WithdrawFee,
  // },
  // {
  //   name: "NFT Marketplace",
  //   layout: "/admin",
  //   path: "/nft-marketplace",
  //   icon: (
  //     <Icon
  //       as={MdOutlineShoppingCart}
  //       width='20px'
  //       height='20px'
  //       color='inherit'
  //     />
  //   ),
  //   component: NFTMarketplace,
  //   secondary: true,
  // },
  // {
  //   name: "Data Tables",
  //   layout: "/admin",
  //   icon: <Icon as={MdBarChart} width='20px' height='20px' color='inherit' />,
  //   path: "/data-tables",
  //   component: DataTables,
  // },
  // {
  //   name: "Profile",
  //   layout: "/admin",
  //   path: "/profile",
  //   icon: <Icon as={MdPerson} width='20px' height='20px' color='inherit' />,
  //   component: Profile,
  // },
  {
    name: "Sign In",
    layout: "/auth",
    path: "/sign-in",
    icon: <Icon as={MdLock} width='20px' height='20px' color='inherit' />,
    component: SignInCentered,
  },
  // {
  //   name: "RTL Admin",
  //   layout: "/rtl",
  //   path: "/rtl-default",
  //   icon: <Icon as={MdHome} width='20px' height='20px' color='inherit' />,
  //   component: RTL,
  // },
];

export default routes;
