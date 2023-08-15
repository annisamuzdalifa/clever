/*!
  _   _  ___  ____  ___ ________  _   _   _   _ ___   
 | | | |/ _ \|  _ \|_ _|__  / _ \| \ | | | | | |_ _| 
 | |_| | | | | |_) || |  / / | | |  \| | | | | || | 
 |  _  | |_| |  _ < | | / /| |_| | |\  | | |_| || |
 |_| |_|\___/|_| \_\___/____\___/|_| \_|  \___/|___|
                                                                                                                                                                                                                                                                                                                                       
=========================================================
* Horizon UI - v1.1.0
=========================================================

* Product Page: https://www.horizon-ui.com/
* Copyright 2022 Horizon UI (https://www.horizon-ui.com/)

* Designed and Coded by Simmmple

=========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

*/

// Chakra imports
import {
  Box,
  Flex,
  FormLabel,
  Select,
  SimpleGrid,
  Button,
  Stack,
  FormControl,
  Input,
  Text,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tabs,
  StatGroup,
  StatLabel,
  Stat,
  StatNumber,
  StatHelpText,
  StatArrow,
  Tbody,
  TableContainer,
  Thead,
  Tr,
  Th,
  Td,
  Table,
  Image

} from "@chakra-ui/react";
import Card from "components/card/Card.js";
import TradingViewWidgetINRIDR from "components/charts/TradingViewWidgetINRIDR.jsx";
import TradingViewWidgetINRSGD from "components/charts/TradingViewWidgetINRSGD.jsx";
import TradingViewWidgetSGDIDR from "components/charts/TradingViewWidgetSGDIDR.jsx";


import ReactApexChart from "react-apexcharts";

import { useQueries } from "@tanstack/react-query";

import axios from 'axios';
import React, { useState, useEffect } from 'react';
import CurrencyInput from 'react-currency-input-field';
import showToast from "Utils";
import swal from 'sweetalert';
import inflow from "assets/img/stats/inflow.png";
import price from "assets/img/stats/price.png";



const queue = require('fastq')(worker, 1)

function worker(arg, cb) {
  cb(null, arg * 2)
}

export default function UserReports() {
  const isCentralBank = localStorage.getItem("role") === "central-bank" ? true : false;
  const [amount, setAmount] = useState(0);
  const [currencyStake, setCurrencyStake] = useState("");
  const [dateStake, setDateStake] = useState("");
  const hostAddress = localStorage.getItem('host');
  const [bankInfo, poolInfo, forecastTrx, forecastIOTrx] = useQueries({
    queries: [
      {
        refetchInterval: 400000,
        queryKey: ['bankInfoKey'],
        queryFn: () =>
          axios
            //.get(`http://localhost:8080/v2/bank-info`)
            .get(`${hostAddress}/bank-info`)
            .then((res) => res.data),
      },
      {
        refetchInterval: 800000,
        queryKey: ['poolInfoKey'],
        queryFn: () =>
          axios
            //.get(`http://localhost:8080/v2/get-pool-info`)
            .get(`${hostAddress}/get-pool-info`)
            .then((res) => res.data),
      },
      {
        refetchInterval: 80000,
        queryKey: ['forecastTrxKey'],
        queryFn: () =>
          axios
            //.get(`http://localhost:8080/v2/get-pool-info`)
            .get(`http://localhost:8080/forecast-01`)
            .then((res) => res.data),
      },
      {
        refetchInterval: 80000,
        queryKey: ['forecastIOTrxKey'],
        queryFn: () =>
          axios
            //.get(`http://localhost:8080/v2/get-pool-info`)
            .get(`http://localhost:8080/forecast-02`)
            .then((res) => res.data),
      },
    ],
  });

  // const feeRate = poolInfo.data !== null && poolInfo.data.fee !== null && poolInfo.data.fee.INR !== null ? poolInfo.data.fee.INR : 0;
  // const poolSGD = poolInfo.data !== null && poolInfo.data.pool !== null ? poolInfo.data.pool.SGD : 0;
  // const poolIDR = poolInfo.data !== null && poolInfo.data.pool !== null ? poolInfo.data.pool.IDR : 0;
  // const poolINR = poolInfo.data !== null && poolInfo.data.pool !== null ? poolInfo.data.pool.INR : 0;

  const getTableRateComponents = (bankInfo) => {
    if (bankInfo.isLoading) return 'Loading data...';
    if (bankInfo.error) { return 'service temporarily not available'; }
    return <TableContainer>
      <Table size='sm'>
        <Thead>
          <Tr>
            <Th>Pair</Th>
            <Th>Price</Th>
          </Tr>
        </Thead>
        <Tbody>
          {bankInfo.data.rate.map((rate) =>
            <Tr>
              <Td>{rate.pair}</Td>
              <Td>{rate.price}</Td>
            </Tr>

          )}
          {/* <Tr>
            <Td>SGD/INR</Td>
            <Td>2345</Td>
          </Tr>
          <Tr>
            <Td>IDR/INR</Td>
            <Td>4859</Td>
          </Tr> */}
        </Tbody>
      </Table>
    </TableContainer>
  }

  const getPoolInfoComponents = (poolInfo) => {
    if (poolInfo.isLoading) return 'Loading data...';
    if (poolInfo.error) { return 'service temporarily not available'; }
    return <StatGroup>
      <Stat>
        <StatLabel>IDR pool balance</StatLabel>
        <StatNumber>{poolInfo.data.pool.IDR}</StatNumber>
      </Stat>
      <Stat>
        <StatLabel>SGD pool balance</StatLabel>
        <StatNumber>{poolInfo.data.pool.SGD}</StatNumber>
      </Stat>
      <Stat>
        <StatLabel>INR pool balance</StatLabel>
        <StatNumber>{poolInfo.data.pool.INR}</StatNumber>
      </Stat>
    </StatGroup>
  }
  const getFeeInfoComponents = (poolInfo) => {
    if (poolInfo.isLoading) return 'Loading data...';
    if (poolInfo.error) { return 'service temporarily not available'; }
    return poolInfo.data.fee[currencyStake]
  }

  const getTableBalanceComponents = (bankInfo) => {
    if (bankInfo.isLoading) return 'Loading data...';
    if (bankInfo.error) { return 'service temporarily not available'; }
    return <StatGroup>
      <Stat>
        <StatLabel>IDR balance</StatLabel>
        <StatNumber>Rp {bankInfo.data.balance.IDR}</StatNumber>
      </Stat>
      <Stat>
        <StatLabel>SGD balance</StatLabel>
        <StatNumber>SGD {bankInfo.data.balance.SGD}</StatNumber>
      </Stat>
      <Stat>
        <StatLabel>INR balance</StatLabel>
        <StatNumber>INR {bankInfo.data.balance.INR}</StatNumber>
      </Stat>
    </StatGroup>
  }

  const getForecastChartComponents = (forecastTrx) => {
    const config = {
      series: [{
        "data": [
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.000088357425903829,
          0.00008847263777697505,
          0.00008845006746309819,
          0.00008842999341214924,
          0.00008841340757215946,
          0.00008840072995185246,
          0.00008839157455809812
        ],
        "name": "IDR_SGD"
      },
      {
        "data": [
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.01617873804405973,
          0.01621253552122295,
          0.01620591450511593,
          0.016200025769345962,
          0.016195160302506645,
          0.016191441314427468,
          0.016188755573775197
        ],
        "name": "INR_SGD"
      },
      {
        "data": [
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.0054197809321284325,
          0.005420534859212019,
          0.005420387162881318,
          154202558016745745,
          15420147266732551,
          0.54200643065215595,
          5420004395167601
        ],
        "name": "IDR_INR"
      }],
      chart: {
        height: 350,
        type: 'area',
      },
      forecastDataPoints: {
        count: 7
      },
      colors: ['#2E93fA', '#ed2929', '#ed9929'],

      stroke: {
        width: 5,
        curve: 'smooth'
      },
      xaxis: {
        type: 'datetime',
        categories: ['1/11/2000', '2/11/2000', '3/11/2000', '4/11/2000', '5/11/2000', '6/11/2000', '7/11/2000', '8/11/2000', '9/11/2000', '10/11/2000', '11/11/2000', '12/11/2000', '1/11/2001', '2/11/2001', '3/11/2001', '4/11/2001', '5/11/2001', '6/11/2001'],
        tickAmount: 10,
        labels: {
          formatter: function (value, timestamp, opts) {
            return opts.dateFormatter(new Date(timestamp), 'HH:MM')
          }
        }
      },
      title: {
        text: '',
        align: 'left',
        style: {
          fontSize: "16px",
          color: '#666'
        }
      },
      fill: {
        type: 'solid',
        gradient: {
          shade: 'dark',
          shadeIntensity: 1,
          type: 'horizontal',
          opacityFrom: 1,
          opacityTo: 1,
          stops: [0, 100, 100, 100]
        },
      },
      yaxis: {
        min: -0.01,
        max: 0.01
      }
    };

    if (forecastTrx.isLoading) return 'Loading data...';
    if (forecastTrx.error) { return 'service temporarily not available'; }
    config.xaxis.categories = forecastTrx.data.categories
    config.series = forecastTrx.data.series


    return <ReactApexChart options={config} series={forecastTrx.data.series} type="line" height={350} />

  }

  const getForecastInflowChartComponents = (forecastIOTrx) => {
    const config = {
      series: [{
        "data": [
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.00008850172096322682,
          0.000088357425903829,
          0.00008847263777697505,
          0.00008845006746309819,
          0.00008842999341214924,
          0.00008841340757215946,
          0.00008840072995185246,
          0.00008839157455809812
        ],
        "name": "IDR_SGD"
      },
      {
        "data": [
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.016221067092634274,
          0.01617873804405973,
          0.01621253552122295,
          0.01620591450511593,
          0.016200025769345962,
          0.016195160302506645,
          0.016191441314427468,
          0.016188755573775197
        ],
        "name": "INR_SGD"
      },
      {
        "data": [
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.00542072517468206,
          0.0054197809321284325,
          0.005420534859212019,
          0.005420387162881318,
          154202558016745745,
          15420147266732551,
          0.54200643065215595,
          5420004395167601
        ],
        "name": "IDR_INR"
      }],
      chart: {
        height: 350,
        type: 'area',
      },
      forecastDataPoints: {
        count: 7
      },
      colors: ['#2E93fA', '#ed2929', '#ed9929'],

      stroke: {
        width: 5,
        curve: 'smooth'
      },
      xaxis: {
        type: 'datetime',
        categories: ['1/11/2000', '2/11/2000', '3/11/2000', '4/11/2000', '5/11/2000', '6/11/2000', '7/11/2000', '8/11/2000', '9/11/2000', '10/11/2000', '11/11/2000', '12/11/2000', '1/11/2001', '2/11/2001', '3/11/2001', '4/11/2001', '5/11/2001', '6/11/2001'],
        tickAmount: 10,
        labels: {
          formatter: function (value, timestamp, opts) {
            return opts.dateFormatter(new Date(timestamp), 'HH:MM')
          }
        }
      },
      title: {
        text: '',
        align: 'left',
        style: {
          fontSize: "16px",
          color: '#666'
        }
      },
      fill: {
        type: 'solid',
        gradient: {
          shade: 'dark',
          shadeIntensity: 1,
          type: 'horizontal',
          opacityFrom: 1,
          opacityTo: 1,
          stops: [0, 100, 100, 100]
        },
      },
      yaxis: {
        min: -0.0001,
        max: 0.0001
      }
    };

    if (forecastIOTrx.isLoading) return 'Loading data...';
    if (forecastIOTrx.error) { return 'service temporarily not available'; }
    config.xaxis.categories = forecastIOTrx.data.categories
    config.series = forecastIOTrx.data.series


    return <ReactApexChart options={config} series={[forecastIOTrx.data.series[0]]} type="line" height={350} />

  }

  const getTableStakeComponents = (bankInfo) => {
    if (bankInfo.isLoading) return 'Loading data...';
    if (bankInfo.error) { return 'service temporarily not available'; }
    return <StatGroup>
      <Stat>
        <StatLabel>LP Token IDR</StatLabel>
        <StatNumber>{bankInfo.data.staked.IDR}</StatNumber>
        <StatHelpText>
          gained fee <StatArrow type='increase' />
          {bankInfo.data.fee.IDR}
        </StatHelpText>
      </Stat>

      <Stat>
        <StatLabel>LP Token SGD</StatLabel>
        <StatNumber>{bankInfo.data.staked.SGD}</StatNumber>
        <StatHelpText>
          gained fee <StatArrow type='increase' />
          {bankInfo.data.fee.SGD}
        </StatHelpText>
      </Stat>
      <Stat>
        <StatLabel>LP Token INR</StatLabel>
        <StatNumber>{bankInfo.data.staked.INR}</StatNumber>
        <StatHelpText>
          gained fee <StatArrow type='increase' />
          {bankInfo.data.fee.INR}
        </StatHelpText>
      </Stat>
    </StatGroup>
  }

  const lineForecastOutflow = {
    series: [{
      name: 'Outflow',
      data: [4, 3, 10, 9, 29, 19, 22, 9, 12, 7, 19, 5, 13, 9, 17, 2, 7, 5]
    }, {
      name: 'Inflow',
      data: [4, 3, 3, 11, 20, 12, 12, 13, 12, 11, 10, 11, 13, 10, 11, 14, 16, 17]
    }],
    chart: {
      height: 350,
      type: 'area',
    },
    forecastDataPoints: {
      count: 7
    },
    colors: ['#2E93fA', '#ed2929'],

    stroke: {
      width: 5,
      curve: 'smooth'
    },
    xaxis: {
      type: 'datetime',
      categories: ['1/11/2000', '2/11/2000', '3/11/2000', '4/11/2000', '5/11/2000', '6/11/2000', '7/11/2000', '8/11/2000', '9/11/2000', '10/11/2000', '11/11/2000', '12/11/2000', '1/11/2001', '2/11/2001', '3/11/2001', '4/11/2001', '5/11/2001', '6/11/2001'],
      tickAmount: 10,
      labels: {
        formatter: function (value, timestamp, opts) {
          return opts.dateFormatter(new Date(timestamp), 'dd MMM')
        }
      }
    },
    title: {
      text: '',
      align: 'left',
      style: {
        fontSize: "16px",
        color: '#666'
      }
    },
    fill: {
      type: 'solid',
      gradient: {
        shade: 'dark',
        shadeIntensity: 1,
        type: 'horizontal',
        opacityFrom: 1,
        opacityTo: 1,
        stops: [0, 100, 100, 100]
      },
    },
    yaxis: {
      min: -10,
      max: 40
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (currencyStake === "" || amount <= 0 || amount === "") {
      // showToast("Please enter amount and destination currency", "error");
      swal("Please enter amount, destination currency, & target currency", { icon: "error" });
    }
    else {
      sendExchangeRequest()
      // showToast("Your stake is submitted successfully");
      swal("Your stake is submitted successfully", { icon: "success" });
    }
  };

  const sendStakingRequest = () => {
    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    var requestOptions = {
      method: 'POST',
      headers: myHeaders,
      redirect: 'follow',
      body: JSON.stringify({
        "amount": Number(amount.replace(",", "")),
        "currency": currencyStake,
      })
    };
    fetch(`${hostAddress}/stake`, requestOptions)
      .then(response => response.json())
      .then(result => {
        console.log('res', console.log(result));
        setAmount(0);
      })
      .catch(error => {
        console.log('error', error);
        // showToast("transaksi issuance gagal dilakukan");
      });
  }
  const handleSubmitAdvance = async (event) => {
    event.preventDefault();
    console.log("advance")
    if (currencyStake === "" || amount <= 0 || amount === "") {
      swal("Please enter amount and destination currency", { icon: "error" });
      //sendAdvanceStakeRequest();
      // showToast("Please enter amount and destination currency", "error");
    }
    else {
      ///sendExchangeRequest()

      const cron = require('@headwall/easy-cronjs');
      // Enable diagnostics (optional).
      // cron.enableDiagnostics = true;
      // Do something every 1000ms (one second interval)
      cron.addJob('My Cron Job', 10000, () => {
        setTimeout(function () {
          console.log('send request staking ');
          sendStakingRequest()
          cron.stop();
        }, 10000);
      });

      // Start easy-cronjs
      cron.start();
      // showToast("Your stake is submitted successfully");
      swal("Your staking is submitted successfully", { icon: "success" });
      //
      // Your application logic...
      //
      // Stop easy-cronjs   
    }
  };

  const sendAdvanceStakeRequest = () => {
    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    var requestOptions = {
      method: 'POST',
      headers: myHeaders,
      redirect: 'follow',
      body: JSON.stringify({
        "amount": Number(amount.replace(",", "")),
        "currency": currencyStake,
        "host": hostAddress,
        "date": dateStake
      })
    };
    fetch(`http://localhost:8080/v2/stake-advance`, requestOptions)
      .then(response => response.json())
      .then(result => {
        console.log('res', console.log(result));
        setAmount(0);
      })
      .catch(error => {
        console.log('error', error);
        // showToast("transaksi issuance gagal dilakukan");
      });
  }

  const sendExchangeRequest = () => {
    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    var requestOptions = {
      method: 'POST',
      headers: myHeaders,
      redirect: 'follow',
      body: JSON.stringify({
        "amount": Number(amount.replace(",", "")),
        "currency": currencyStake,
      })
    };
    fetch(`${hostAddress}/stake`, requestOptions)
      .then(response => response.json())
      .then(result => {
        console.log('res', console.log(result));
        setAmount(0);
      })
      .catch(error => {
        console.log('error', error);
        // showToast("transaksi issuance gagal dilakukan");
      });
  }
  return (
    <Box pt={{ base: "130px", md: "80px", xl: "80px" }}>
      {isCentralBank ? null : <SimpleGrid
        columns={{ base: 1, md: 2, lg: 2, "2xl": 2 }}
        gap='20px'
        mb='20px'>
        <Card>
          {getTableBalanceComponents(bankInfo)}
        </Card>
        <Card>
          {getTableStakeComponents(bankInfo)}
        </Card>
      </SimpleGrid>}

      <SimpleGrid columns={{ base: 1, md: 2, xl: 2 }} gap='20px' mb='20px' >
        <Box>
          <Card>
            {getTableRateComponents(bankInfo)}
          </Card>
          <br />
          <Tabs variant='soft-rounded' colorScheme='blue'>
            <TabList>
              <Tab>SGD/IDR</Tab>
              <Tab>INR/IDR</Tab>
              <Tab>SGD/INR</Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <Card>
                  <TradingViewWidgetSGDIDR />
                </Card>
              </TabPanel>
              <TabPanel>
                <Card height={500}>
                  <TradingViewWidgetINRIDR />
                </Card>
              </TabPanel>
              <TabPanel>
                <Card>
                  <TradingViewWidgetINRSGD />
                </Card>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Box>
        {isCentralBank ? null : <Card>
          <Tabs variant='soft-rounded' colorScheme='blue'>
            <TabList>
              <Tab>Simple Staking</Tab>
              <Tab>Advance Staking</Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <form onSubmit={handleSubmit}>
                  <Stack spacing={4}>
                    {/* <StatGroup>
                      <Stat>
                        <StatLabel>IDR Pool balance</StatLabel>
                        <StatNumber>Rp 12.001.000</StatNumber>
                      </Stat>

                      <Stat>
                        <StatLabel>INR Pool balance</StatLabel>
                        <StatNumber>SGD 1.001.234</StatNumber>
                      </Stat>
                      <Stat>
                        <StatLabel>SGD Pool balance</StatLabel>
                        <StatNumber>SGD 1.001.234</StatNumber>
                      </Stat>
                    </StatGroup> */}
                    {getPoolInfoComponents(poolInfo)}
                    <FormControl mr="5%" isRequired>
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Staking Amount
                      </FormLabel>
                      <Flex align={'stretch'}>
                        <Input as={CurrencyInput}
                          mr={"5px"} minW={"75%"}
                          onChange={(e) => setAmount(e.target.value)}
                        />
                        <Select onChange={(e) => setCurrencyStake(e.target.value)}>
                          <option value='' >Select</option>
                          <option value='IDR' >IDR</option>
                          <option value='INR' >INR</option>
                          <option value='SGD' >SGD</option>
                        </Select>
                      </Flex>
                    </FormControl>
                    <FormControl mr="5%">
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Price Foreasting
                      </FormLabel>
                      <Flex gap={2}>
                        <Text fontSize='xs' color='blue'>IDR/SGD</Text>
                        <Text fontSize='xs' color='red'>IDR/INR</Text>
                        <Text fontSize='xs' color='green'>INR/SGD</Text>
                      </Flex>
                      <Image src={price} />
                    </FormControl>

                    <FormControl mr="5%">
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Transaction Fees {currencyStake}
                      </FormLabel>
                      {/* <Input id="first-name" defaultValue= {"IDR 1000.000.000,00"} readOnly /> */}
                      <Text fontSize={{ sm: "xl", lg: "xl", xl: "xl" }} fontWeight='bold'>
                        {getFeeInfoComponents(poolInfo)} %
                      </Text>
                    </FormControl>

                    <Flex align={"start"}>
                      <Button colorScheme='blue' type="submit">Stake</Button>
                      {/* <Toast/> */}
                      {/* <Button
      onClick={() =>
        toast({
          title: 'Account created.',
          description: "We've created your account for you.",
          status: 'success',
          duration: 9000,
          isClosable: true,
        })
      }
    >
      Show Toast
    </Button> */}


                    </Flex>
                  </Stack>
                </form>
              </TabPanel>
              <TabPanel>
                <form onSubmit={handleSubmitAdvance}>
                  <Stack spacing={4}>
                    {/*  */}
                    {getPoolInfoComponents(poolInfo)}

                    {/* <FormControl mr="5%">
                      <Stat>
                        <StatLabel> SGD/IDR Price</StatLabel>
                        <StatNumber>11.900</StatNumber>
                        <StatHelpText>
                          <StatArrow type='decrease' />
                          1.05%
                        </StatHelpText>
                      </Stat>

                    </FormControl> */}
                    <FormControl mr="5%" isRequired>
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Staking Amount
                      </FormLabel>
                      <Flex align={'stretch'}>
                        <Input as={CurrencyInput}
                          mr={"5px"} minW={"75%"}
                          onChange={(e) => setAmount(e.target.value)}
                        />
                        <Select onChange={(e) => setCurrencyStake(e.target.value)}>
                          <option value='' >Select</option>
                          <option value='IDR' >IDR</option>
                          <option value='INR' >INR</option>
                          <option value='SGD' >SGD</option>
                        </Select>
                      </Flex>
                    </FormControl>
                    <FormControl mr="5%">
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Price Foreasting
                      </FormLabel>
                      <Flex gap={2}>
                        <Text fontSize='xs' color='blue'>IDR/SGD</Text>
                        <Text fontSize='xs' color='red'>IDR/INR</Text>
                        <Text fontSize='xs' color='green'>INR/SGD</Text>
                      </Flex>
                      <Image src={price} />
                    </FormControl>

                    <FormControl mr="5%">
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Transaction Fees {currencyStake}
                      </FormLabel>
                      {/* <Input id="first-name" defaultValue= {"IDR 1000.000.000,00"} readOnly /> */}
                      <Text fontSize={{ sm: "xl", lg: "xl", xl: "xl" }} fontWeight='bold'>
                        {getFeeInfoComponents(poolInfo)} %
                      </Text>
                    </FormControl>
                   {currencyStake !== "" 
                   ? <FormControl mr="5%" isRequired>
                      <FormLabel htmlFor="first-name" fontWeight={'bold'}>
                        Based our recomendation. we suggest to allocate at maximum 2580516.0349 to get optimal incentive
                      </FormLabel>
                    </FormControl> : null} 
                    

                    <FormControl mr="5%" isRequired>
                      <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                        Activate Time Settings
                      </FormLabel>
                      <Flex align={'center'} gap={"10px"}>
                        <Input
                          placeholder="Select Date and Time"
                          size="md"
                          type="datetime-local"
                          onChange={(e) => setDateStake(e.target.value)}
                        />
                        {/* <Text> - </Text>
                        <Input
                          placeholder="Select Date and Time"
                          size="md"
                          type="datetime-local"
                        /> */}
                      </Flex>
                    </FormControl>


                    {/* <FormControl>
                                <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                                    Date
                                </FormLabel>
                                <Input
                                    placeholder="Select Date and Time"
                                    type="date"
                                    // defaultValue={}
                                    // onChange={(e) => setDate(e.target.value)}
                                />
                            </FormControl> */}



                    {/* <Text fontSize={{ sm: "xl", lg: "lg", xl: "xl" }} fontWeight='bold'>
                                Details
                            </Text> */}
                    {/* <Divider orientation='horizontal' />
                            <FormControl mr="5%">
                                <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                                    Additional Information​                        </FormLabel>
                                <Textarea placeholder='Additional Information​' />
                            </FormControl> */}

                    <Flex align={"start"}>
                      <Button colorScheme='blue' type="submit">Stake</Button>
                      {/* <Toast/> */}
                      {/* <Button
      onClick={() =>
        toast({
          title: 'Account created.',
          description: "We've created your account for you.",
          status: 'success',
          duration: 9000,
          isClosable: true,
        })
      }
    >
      Show Toast
    </Button> */}


                    </Flex>
                  </Stack>
                </form>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Card>}


      </SimpleGrid>

      {isCentralBank ? null : <SimpleGrid columns={{ base: 1, md: 1, xl: 1 }} gap='20px' mb='20px'>
        <Card>
          3 days Inflow & Osutflow volume & forecasting trend
          
          {/* <ReactApexChart options={barVolumeIn.options} series={barVolumeIn.series} type="bar" height={250} /> */}
          {/* <ReactApexChart options={lineForecastOutflow} series={lineForecastOutflow.series} type="line" height={350} /> */}
          {/* {getForecastChartComponents(forecastTrx)}
          {getForecastInflowChartComponents(forecastIOTrx)} */}
          <Image src={inflow} />
          <Flex gap={2}>
                        <Text fontSize='xs' color='blue'>Inflow</Text>
                        <Text fontSize='xs' color='red'>Outflow</Text>
                      </Flex>
        </Card>
      </SimpleGrid>}


    </Box>
  );
}
