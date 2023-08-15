
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
import React from "react";
import {
} from "react-icons/md";
import {
} from "views/admin/default/variables/columnsData";
import { useState } from "react";
import showToast from "Utils";
import swal from 'sweetalert';


export default function Issuance() {

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
        "recipient": recipient
      })
    };
    //fetch(`http://localhost:8080/v2/issue-${currencyDest.toLowerCase()}`, requestOptions)
    fetch(`${hostAddress}/issue-${currencyDest.toLowerCase()}`, requestOptions)
      .then(response => response.json())
      .then(result => {
        console.log('res', console.log(result));
        setAmount(0);
        showToast("Issuance request is successfull");
        swal("Issuance request is successfull",{icon:"success"});

      })
      .catch(error => {
        console.log('error', error);
        showToast("transaksi issuance gagal dilakukan", "error");
        swal("transaksi issuance gagal dilakukan", {icon:"error"});

      });
      setAmount("");
      setRecipient("");
      setCurrencyDest("");
  }

  const handleSubmit = (event) => {
    event.preventDefault();
    if (currencyDest === "" || amount <= 0 || amount === "") {
      showToast("Please enter amount, destination currency, ", "error");
      swal("Please enter amount, destination currency, ", {icon:"error"});
    }
    else {
      sendExchangeRequest()
    }
  };

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
                  /><Select placeholder='Currencies'  onChange={(e) => setCurrencyDest(e.target.value)}>
                    <option value='IDR' defaultChecked>IDR</option>
                    <option value='INR' defaultChecked>INR</option>
                    <option value='SGD' defaultChecked>SGD</option>
                  </Select>
                </Flex>
              </FormControl>
              <FormControl mr="5%" isRequired>
                <FormLabel htmlFor="first-name" fontWeight={'normal'}>
                  Recipients
                </FormLabel>
                <Select placeholder='Select'
                  onChange={(e) => setRecipient(e.target.value)}>
                  <option value="O=INDOIDJA, L=Jakarta, C=ID">INDOIDJA</option>
                  <option value="O=CENAIDJA,L=Jakarta, C=ID">CENAIDJA</option>
                  <option value="O=DBSSSGSG, L=Singapore, C=SG">DBSSSGSG</option>
                  <option value="O=HDFCINBB, L=Mumbai, C=IN">HDFCINBB</option>
                </Select>
              </FormControl>
              <Flex align={"start"}>
                <Button colorScheme='blue' type="submit">Create Issuance</Button>
              </Flex>
            </Stack>
          </form>
        </Card>
      </SimpleGrid>
    </Box>
  );
}
