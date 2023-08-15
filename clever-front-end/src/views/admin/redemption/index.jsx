
import CurrencyInput from 'react-currency-input-field';
import {
  Box,
  Flex,
  FormLabel,
  Select,
  SimpleGrid,
  Button,
  Stack,
  FormControl,
  Input

} from "@chakra-ui/react";
import Card from "components/card/Card.js";
// Assets
// Custom components
import {
} from "react-icons/md";
import {
} from "views/admin/default/variables/columnsData";
import showToast from "Utils";
import React, { useState } from 'react';
import { useQueries } from "@tanstack/react-query";
import axios from 'axios';
import swal from 'sweetalert';

export default function Redemption() {

  // Chakra Color Mode
  const [amount, setAmount] = useState(0);
  const [currencyDest, setCurrencyDest] = useState("");
  const [recipient, setRecipient] = useState("");
  const hostAddress = localStorage.getItem('host');

  const sendExchangeRequest = () => {
    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    var requestOptions = {
      method: 'POST',
      headers: myHeaders,
      redirect: 'follow',
      body: JSON.stringify({
        "amount": Number(amount.replace(",", "")),
        "currency": currencyDest
      })
    };
    //fetch(`http://localhost:8080/v2/redeem-Fiat`, requestOptions)
    fetch(`${hostAddress}/redeem-Fiat`, requestOptions)
      .then(response => response.json())
      .then(result => {
        console.log('res', console.log(result));
        setAmount(0);
        showToast("Redemption request is successfull");
        swal("Redemption request is successfull",{icon:"success"});
      })
      .catch(error => {
        console.log('error', error);
        showToast("Redemption request is failed", "error");
        swal("Redemption request is failed",{icon:"error"});
      });
  }

  const [bankInfo] = useQueries({
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
    ],
  });

  const handleSubmit = (event) => {
    event.preventDefault();
    if (currencyDest === "" || amount <= 0 || amount === "") {
      showToast("Please enter amount, destination currency, ", "error");
      swal("Please enter amount, destination currency",{icon:"error"});
    }
    else {
      sendExchangeRequest()
    }
  };

  const getBalanceComponents = (bankInfo) => {
    if (bankInfo.isLoading) return 'Loading data...';
    if (bankInfo.error) { return 'service temporarily not available'; }
    if (currencyDest === "") {return 0}
    return bankInfo.data.balance[currencyDest]
  }

  return (
    <Box pt={{ base: "130px", md: "80px", xl: "80px" }}>
      <SimpleGrid columns={{ base: 1, md: 2, xl: 2 }} gap='20px' mb='20px'>
        <Card>
          <form onSubmit={handleSubmit}>
            <Stack spacing={4}>
              <FormControl mr="5%" isRequired>
                <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                  Amount
                </FormLabel>
                <Flex align={'stretch'}>
                  <Input as={CurrencyInput}
                    mr={"5px"} minW={"75%"}
                    onChange={(e) => setAmount(e.target.value)}
                  /><Select placeholder='Currencies' defaultValue={""} onChange={(e) => setCurrencyDest(e.target.value)}>
                    <option value='IDR' defaultChecked>IDR</option>
                    <option value='INR' defaultChecked>INR</option>
                    <option value='SGD' defaultChecked>SGD</option>
                  </Select>
                </Flex>
                Available Balance : {currencyDest} {getBalanceComponents(bankInfo)}

              </FormControl>
              {/* <FormControl mr="5%" isRequired>
                <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                  Recipients
                </FormLabel>
                <Select placeholder='Select'
                  onChange={(e) => setRecipient(e.target.value)}>
                  <option value="IRB">BANK IRB</option>
                </Select>
              </FormControl> */}
              <Flex align={"start"}>
                <Button colorScheme='blue' type="submit">Create Redemption</Button>
              </Flex>
            </Stack>
          </form>
        </Card>
      </SimpleGrid>
    </Box>
  );
}
